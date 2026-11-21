"""Device and Gradle side of tsctl. Each leaf command declares a Policy; main() gates centrally."""

import argparse
import datetime
import json
import os
import re
import shlex
import shutil
import signal
import subprocess
import sys
import tempfile
import time
from dataclasses import dataclass, field
from pathlib import Path

from core import (
    BASE_PACKAGE, NO_DEVICE, NO_DEVICE_DRY, PROTECTED_PACKAGE, VARIANTS, Failed, Policy, Refused,
    UsageError, apply_pref_edits, expired, check_dry_run, classify, decide, describe_node, flatten_layout,
    node_matches, parse_focused_package, parse_granted_permissions, parse_input_results,
    parse_installed_info, parse_prefs, validate_grant, parse_string_resources, parse_tool_ids, positive_int,
    prefs_dict, prune_state, resolve_pref_key, resolve_tool, serialize_prefs, shard,
)

REPO = Path(__file__).resolve().parents[4]
MAIN_ACTIVITY = "com.kylecorry.trail_sense.main.MainActivity"
OPEN_TOOL_ACTION = "com.kylecorry.trail_sense.OPEN_TOOL"
DATABASE = "trail_sense"
DEFAULT_AVD = "Android_16"
DEFAULT_GPU = "swiftshader_indirect"
# With swiftshader the host memory grows ~125 MB per UI session; restart above this.
RSS_RESTART_MB = 4500
STATE_DIR = Path(os.environ.get("XDG_CACHE_HOME", Path.home() / ".cache")) / "tsctl"
CAPTURES = REPO / "captures" / "verify"
BACKUPS = CAPTURES / "backups"
# Folders under these hold one backup each and age separately; every other folder in CAPTURES is one run.
BACKUP_FOLDERS = ("backups", "db-backups")
RETENTION_DAYS = 7

PERMISSION_ALIASES = {
    "location": ["android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"],
    "background-location": ["android.permission.ACCESS_BACKGROUND_LOCATION"],
    "camera": ["android.permission.CAMERA"],
    "notifications": ["android.permission.POST_NOTIFICATIONS"],
    "activity": ["android.permission.ACTIVITY_RECOGNITION"],
}
APPOP_ALIASES = {"exact-alarm": "SCHEDULE_EXACT_ALARM"}
ALL_PERMISSIONS = ["location", "background-location", "camera", "notifications", "activity", "exact-alarm"]
EMULATOR_SENSORS = [
    "acceleration", "gyroscope", "magnetic-field", "orientation", "temperature", "proximity",
    "light", "pressure", "humidity", "magnetic-field-uncalibrated", "gyroscope-uncalibrated",
]
# Matches TestUtils.setupDefaultPreferences, minus its test-only compact mode and bottom navigation.
SEED_PREFS = [
    ("@pref_altimeter_calibration_mode", "string", "gps"),
    ("@pref_altimeter_accuracy", "int", "1"),
    ("@pref_gps_accuracy_meters", "float", "0"),
    ("@pref_gps_accuracy_wait_seconds", "int", "0"),
    ("@pref_onboarding_completed", "bool", "true"),
    ("@pref_main_disclaimer_shown_key", "bool", "true"),
    ("@pref_distance_units", "string", "feet_miles"),
    ("@pref_weight_units", "string", "lbs"),
    ("@pref_use_24_hour", "bool", "false"),
    ("@pref_cliff_height_enabled", "bool", "true"),
]


@dataclass
class Out:
    data: object
    text: str = ""
    code: int = 0


# ---------------------------------------------------------------- process helpers

def log(msg):
    print(msg, file=sys.stderr)


def run(cmd, check=True, input_bytes=None, env=None, timeout=None):
    result = subprocess.run(cmd, input=input_bytes, capture_output=True, env=env, timeout=timeout)
    if check and result.returncode != 0:
        err = (result.stderr or result.stdout).decode(errors="replace").strip()
        raise Failed(f"`{shlex.join(cmd)}` exited {result.returncode}\n{err}")
    return result


def text(result):
    return result.stdout.decode(errors="replace")


def adb_serials():
    out = text(run(["adb", "devices"]))
    return [l.split()[0] for l in out.splitlines()[1:] if l.strip().endswith("\tdevice")]


def getprop(serial, prop):
    return text(run(["adb", "-s", serial, "shell", "getprop", prop], check=False)).strip()


def classify_serial(serial):
    props = [getprop(serial, "ro.kernel.qemu"), getprop(serial, "ro.boot.qemu")] if serial.startswith("emulator-") else []
    return classify(serial, props, os.environ.get("TSCTL_FORCE_PHYSICAL") == "1")


class Target:
    def __init__(self, serial):
        self.serial = serial
        self.emulator, self.reason = classify_serial(serial)

    def adb(self, *args):
        return ["adb", "-s", self.serial, *args]

    def shell(self, *args, check=True):
        return run(self.adb("shell", *args), check=check)


# ---------------------------------------------------------------- context

@dataclass
class Ctx:
    """Global options parsed once at the boundary, plus the resolved target and planned dry-run steps."""
    args: argparse.Namespace
    device: str = None
    variant: str = "debug"
    dry_run: bool = False
    approved: str = None
    json: bool = False
    target: Target = None
    planned: list = field(default_factory=list)

    @property
    def package(self):
        return VARIANTS[self.variant].package

    def mutate(self, cmd, **kw):
        if self.dry_run:
            self.planned.append(shlex.join(cmd))
            return None
        return run(cmd, **kw)

    def plan(self, description):
        self.planned.append(description)


def resolve_target(requested):
    serials = adb_serials()
    if requested:
        if requested not in serials:
            raise Failed(f"Device {requested} is not connected. Connected: {serials or 'none'}. Run `tsctl devices`.")
        return Target(requested)
    emulators = [s for s in serials if classify_serial(s)[0]]
    if not emulators:
        raise Failed("No emulator running. Start one with `tsctl emu start`. "
                     "Physical devices are only used with an explicit --device.")
    if len(emulators) > 1:
        raise Failed(f"Several emulators running ({', '.join(emulators)}); pass --device.")
    return Target(emulators[0])


# ---------------------------------------------------------------- app helpers

def installed_info(target, package):
    return parse_installed_info(text(target.shell("dumpsys", "package", package, check=False)), package)


def require_debuggable(ctx):
    info = installed_info(ctx.target, ctx.package)
    if not info:
        raise Failed(f"{ctx.package} is not installed on {ctx.target.serial}. Run `tsctl app install`.")
    if not info["debuggable"]:
        raise Failed(f"{ctx.package} is not debuggable, so run-as (prefs/db) is unavailable. Use --variant debug or nightly.")


def run_as(ctx, *cmd, check=True):
    return run(ctx.target.adb("exec-out", "run-as", ctx.package, *cmd), check=check)


def write_app_file(ctx, rel_path, data):
    """Writes atomically: stage in /data/local/tmp, copy to a temp file beside the target, rename.

    adb exec-in can return before the device finishes writing, so it isn't used here.
    """
    if ctx.dry_run:
        ctx.plan(f"write {len(data)} bytes to {ctx.package}:{rel_path}")
        return
    staged = f"/data/local/tmp/tsctl-{os.getpid()}-{time.time_ns()}"
    tmp = f"{rel_path}.tsctl-tmp"
    with tempfile.NamedTemporaryFile() as local:
        local.write(data)
        local.flush()
        run(ctx.target.adb("push", local.name, staged))
    try:
        run(ctx.target.adb("shell", "chmod", "644", staged))
        parent = shlex.quote(os.path.dirname(rel_path) or ".")
        script = (f"mkdir -p {parent} && cat {staged} > {shlex.quote(tmp)} && chmod 660 {shlex.quote(tmp)} && "
                  f"[ $(wc -c < {shlex.quote(tmp)}) -eq {len(data)} ] && mv -f {shlex.quote(tmp)} {shlex.quote(rel_path)}")
        result = run_as(ctx, "sh", "-c", script, check=False)
        if result.returncode != 0:
            run_as(ctx, "rm", "-f", tmp, check=False)
            raise Failed(f"Could not write {rel_path}; the original is untouched.\n{text(result).strip()}")
    finally:
        run(ctx.target.adb("shell", "rm", "-f", staged), check=False)


def force_stop(ctx):
    ctx.mutate(ctx.target.adb("shell", "am", "force-stop", ctx.package))


def launch_cmd(ctx, extra=()):
    return ctx.target.adb("shell", "am", "start", "-W", "-n", f"{ctx.package}/{MAIN_ACTIVITY}", *extra)


def tool_ids():
    return parse_tool_ids((REPO / "app/src/main/java/com/kylecorry/trail_sense/tools/tools/infrastructure/Tools.kt").read_text())


def string_resources():
    return parse_string_resources((REPO / "app/src/main/res/values/strings.xml").read_text())


def prefs_path(ctx):
    return f"shared_prefs/{ctx.package}_preferences.xml"


def read_prefs(ctx):
    result = run_as(ctx, "cat", prefs_path(ctx), check=False)
    return parse_prefs(result.stdout) if result.returncode == 0 else None


def edit_prefs(ctx, edits):
    require_debuggable(ctx)
    root = read_prefs(ctx)
    if root is None:
        raise Failed(f"{prefs_path(ctx)} does not exist yet. Launch the app once (`tsctl app launch`) or run `tsctl app seed`.")
    strings = string_resources()
    changes = apply_pref_edits(root, [(resolve_pref_key(k, strings), kind, v) for k, kind, v in edits])
    # The app caches prefs in memory and would overwrite the file, so stop it first.
    force_stop(ctx)
    write_app_file(ctx, prefs_path(ctx), serialize_prefs(root))
    return changes


def changes_text(changes):
    return "\n".join(f"{c['key']}: {c['before'] if c['before'] is not None else '<unset>'} -> "
                     f"{c['after'] if c['after'] is not None else '<deleted>'}" for c in changes)


def pull_app_file(ctx, rel_path, dest):
    result = run_as(ctx, "cat", rel_path, check=False)
    if result.returncode != 0 or not result.stdout or result.stdout.startswith(b"cat:"):
        return False
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(result.stdout)
    return True


def pull_db(ctx, dest):
    for suffix in ("", "-wal", "-shm"):
        pull_app_file(ctx, f"databases/{DATABASE}{suffix}", dest / f"{DATABASE}{suffix}")
    if not (dest / DATABASE).exists():
        raise Failed(f"No databases/{DATABASE} for {ctx.package}. Launch the app once to create it.")
    return dest / DATABASE


def backup_app_data(ctx, reason, package=None):
    """Copies the app's shared prefs and database to captures/verify/backups/<time>-<reason>.

    Returns the backup directory, or None when there is nothing to back up. Force-stops the app
    first so the copy is consistent.
    """
    package = package or ctx.package
    if ctx.dry_run:
        ctx.plan(f"back up {package} prefs and database to {BACKUPS.relative_to(REPO)}/<time>-{reason}")
        return None
    info = installed_info(ctx.target, package)
    if not info:
        return None
    if not info["debuggable"]:
        log(f"warning: {package} is not debuggable, so its data can't be backed up before {reason}")
        return None
    backup_ctx = Ctx(args=ctx.args, target=ctx.target, variant=next(v for v, var in VARIANTS.items() if var.package == package))
    run(ctx.target.adb("shell", "am", "force-stop", package))
    dest = BACKUPS / f"{datetime.datetime.now().strftime('%Y%m%d-%H%M%S')}-{reason}"
    names = text(run_as(backup_ctx, "ls", "shared_prefs", check=False)).split()
    files = [f"shared_prefs/{n}" for n in names if n.endswith(".xml")]
    files += [f"databases/{DATABASE}{suffix}" for suffix in ("", "-wal", "-shm")]
    saved = [f for f in files if pull_app_file(backup_ctx, f, dest / f)]
    if not saved:
        return None
    (dest / "manifest.json").write_text(json.dumps(
        {"package": package, "serial": ctx.target.serial, "reason": reason, "files": saved}, indent=2))
    return dest


def backup_line(backup):
    return f"backup: {backup} (undo with `tsctl app restore {backup}`)" if backup else ""


def sqlite(db_path, sql, json_out=False):
    return text(run(["sqlite3"] + (["-json"] if json_out else ["-header", "-column"]) + [str(db_path), sql]))


def sqlite_rows(db_path, sql):
    out = sqlite(db_path, sql, json_out=True).strip()
    return json.loads(out) if out else []


# ---------------------------------------------------------------- capture retention

def newest_mtime(path):
    newest = path.lstat().st_mtime
    if path.is_dir() and not path.is_symlink():
        for root, dirs, files in os.walk(path):
            for name in dirs + files:
                newest = max(newest, os.lstat(os.path.join(root, name)).st_mtime)
    return newest


def prune_captures(dry_run):
    """Deletes run folders and backups in captures/verify untouched for more than RETENTION_DAYS."""
    if not CAPTURES.is_dir():
        return []
    entries = {}
    for child in CAPTURES.iterdir():
        if child.name in BACKUP_FOLDERS and child.is_dir():
            entries.update({b: newest_mtime(b) for b in child.iterdir()})
        else:
            entries[child] = newest_mtime(child)
    old = expired(entries, time.time(), RETENTION_DAYS * 86400)
    if dry_run:
        return old
    for path in old:
        if path.is_dir() and not path.is_symlink():
            shutil.rmtree(path)
        else:
            path.unlink()
    for name in BACKUP_FOLDERS:
        folder = CAPTURES / name
        if folder.is_dir() and not any(folder.iterdir()):
            folder.rmdir()
    return old


# ---------------------------------------------------------------- emulator helpers

def state_path():
    return STATE_DIR / "emulators.json"


def pid_alive(pid):
    try:
        os.kill(pid, 0)
        return True
    except OSError:
        return False


def load_state():
    try:
        raw = json.loads(state_path().read_text())
    except (OSError, ValueError):
        return {}
    alive = {e["pid"] for e in raw.values() if pid_alive(e.get("pid", -1))}
    state = prune_state(raw, alive, set(adb_serials()))
    if state != raw:
        save_state(state)
    return state


def save_state(state):
    STATE_DIR.mkdir(parents=True, exist_ok=True)
    tmp = state_path().with_suffix(".tmp")
    tmp.write_text(json.dumps(state, indent=2))
    tmp.replace(state_path())


def grants_path():
    return STATE_DIR / "grants.json"


def load_grants():
    try:
        return json.loads(grants_path().read_text())
    except (OSError, ValueError):
        return {}


def save_grants(grants):
    STATE_DIR.mkdir(parents=True, exist_ok=True)
    tmp = grants_path().with_suffix(".tmp")
    tmp.write_text(json.dumps(grants, indent=2, sort_keys=True))
    tmp.replace(grants_path())


def emulator_avd(serial):
    out = text(run(["adb", "-s", serial, "emu", "avd", "name"], check=False))
    return out.splitlines()[0].strip() if out.strip() else None


def qemu_rss_mb(avd):
    out = text(run(["ps", "-eo", "rss=,args="], check=False))
    for line in out.splitlines():
        rss, _, args = line.strip().partition(" ")
        if "qemu-system" in args and re.search(rf"-avd {re.escape(avd or '')}(\s|$)", args):
            return int(rss) // 1024
    return None


def emulator_binary():
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or str(Path.home() / "Android/Sdk")
    path = Path(sdk) / "emulator" / "emulator"
    return str(path) if path.exists() else (shutil.which("emulator") or "emulator")


def gradle(ctx, tasks, serial=None, timeout=None):
    env = dict(os.environ)
    if serial:
        env["ANDROID_SERIAL"] = serial
    cmd = ["./gradlew", *tasks]
    if ctx.dry_run:
        ctx.plan((f"ANDROID_SERIAL={serial} " if serial else "") + shlex.join(cmd))
        return 0
    log("$ " + shlex.join(cmd))
    try:
        # Gradle output goes to stderr so stdout stays parseable with --json.
        return subprocess.run(cmd, cwd=REPO, env=env, timeout=timeout, stdout=sys.stderr).returncode
    except subprocess.TimeoutExpired:
        log(f"Timed out after {timeout}s")
        return 124


def assemble_task(variant):
    return "assemble" + variant[0].upper() + variant[1:]


def latest_apk(variant):
    folder = REPO / "app/build/outputs/apk" / variant
    apks = sorted(folder.glob("*.apk"), key=lambda p: p.stat().st_mtime, reverse=True) if folder.exists() else []
    return apks[0] if apks else None


# ---------------------------------------------------------------- commands: overview

def cmd_devices(ctx):
    rows = []
    for serial in adb_serials():
        is_emu, reason = classify_serial(serial)
        rows.append({"serial": serial, "kind": "emulator" if is_emu else "physical", "reason": reason,
                     "avd": emulator_avd(serial) if is_emu else None,
                     "model": None if is_emu else getprop(serial, "ro.product.model")})
    lines = [f"{r['serial']}\t{r['kind'].upper() if r['kind'] == 'physical' else r['kind']}\t{r['reason']}\t{r['avd'] or r['model']}" for r in rows]
    return Out(rows, "\n".join(lines) or "no devices")


def cmd_doctor(ctx):
    t = ctx.target
    report = {"serial": t.serial, "kind": "emulator" if t.emulator else "physical", "reason": t.reason,
              "android": getprop(t.serial, "ro.build.version.release"), "api": getprop(t.serial, "ro.build.version.sdk"),
              "boot_completed": getprop(t.serial, "sys.boot_completed") == "1", "variant": ctx.variant,
              "package": ctx.package, "problems": []}
    if not report["boot_completed"]:
        report["problems"].append("boot not completed")
    if t.emulator:
        avd = emulator_avd(t.serial)
        report.update(avd=avd, started_by_tsctl=t.serial in load_state(), host_rss_mb=qemu_rss_mb(avd))
        if (report["host_rss_mb"] or 0) > RSS_RESTART_MB:
            report["problems"].append(f"emulator host memory above {RSS_RESTART_MB} MB: restart it (`tsctl emu stop`, `tsctl emu start`)")
    info = installed_info(t, ctx.package)
    report["installed"] = info
    if not info:
        report["problems"].append("app not installed: run `tsctl app install`")
    else:
        apk = latest_apk(ctx.variant)
        report["local_apk"] = {"path": str(apk.relative_to(REPO)), "built": datetime.datetime.fromtimestamp(apk.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S")} if apk else None
        if not t.emulator:
            report["onboarding_completed"] = None
        elif info["debuggable"]:
            root = read_prefs(ctx)
            done = root is not None and prefs_dict(root).get("onboarding_completed", {}).get("value") == "true"
            report["onboarding_completed"] = done
            if not done:
                report["problems"].append("onboarding not completed: run `tsctl app seed`")
    report["focused_window"] = parse_focused_package(text(t.shell("dumpsys", "window", check=False)))
    report["ok"] = not report["problems"]
    lines = [f"target: {t.serial} ({report['kind']}: {t.reason})",
             f"android: {report['android']} (api {report['api']}), boot completed: {report['boot_completed']}"]
    if t.emulator:
        lines.append(f"avd: {report['avd']}; started by tsctl: {report['started_by_tsctl']}; host memory: {report['host_rss_mb']} MB")
    lines.append(f"variant: {ctx.variant} -> {ctx.package}")
    if info:
        lines.append(f"installed: {info.get('versionName')} (code {info.get('versionCode')}), updated {info.get('lastUpdateTime')}, debuggable={info['debuggable']}")
        if report.get("local_apk"):
            lines.append(f"local apk: {report['local_apk']['path']} built {report['local_apk']['built']}")
        if "onboarding_completed" in report:
            lines.append("onboarding completed: " + ("not checked (reading app data on a physical device needs approval)"
                                                     if report["onboarding_completed"] is None else str(report["onboarding_completed"])))
    lines.append(f"focused window: {report['focused_window'] or '?'}")
    lines += [f"problem: {p}" for p in report["problems"]]
    lines.append("doctor: OK" if report["ok"] else "doctor: NOT READY")
    return Out(report, "\n".join(lines), 0 if report["ok"] else 1)


# ---------------------------------------------------------------- commands: device grants

def cmd_device_grant(ctx):
    serial, package = ctx.args.serial, ctx.args.package
    validate_grant(package)
    if serial not in adb_serials():
        raise Failed(f"{serial} is not connected. Run `tsctl devices`.")
    if classify_serial(serial)[0]:
        raise UsageError(f"{serial} is an emulator; emulators never need approval.")
    grants = load_grants()
    grants[serial] = sorted(set(grants.get(serial, [])) | {package})
    save_grants(grants)
    return Out(grants, f"full control of {package} on {serial}: tsctl runs commands for it without --user-approved")


def cmd_device_revoke(ctx):
    grants = load_grants()
    remaining = [p for p in grants.get(ctx.args.serial, []) if p != ctx.args.package]
    if remaining:
        grants[ctx.args.serial] = remaining
    else:
        grants.pop(ctx.args.serial, None)
    save_grants(grants)
    return Out(grants, f"revoked {ctx.args.package} on {ctx.args.serial}")


def cmd_device_grants(ctx):
    grants = load_grants()
    return Out(grants, "\n".join(f"{s}\t{p}" for s, ps in grants.items() for p in ps) or "no grants")


# ---------------------------------------------------------------- commands: emulator

def cmd_emu_list(ctx):
    avds = [l.strip() for l in text(run([emulator_binary(), "-list-avds"])).splitlines() if l.strip()]
    return Out(avds, "\n".join(avds))


def cmd_emu_status(ctx):
    state = load_state()
    rows = []
    for serial in adb_serials():
        if classify_serial(serial)[0]:
            avd = emulator_avd(serial)
            rss = qemu_rss_mb(avd)
            rows.append({"serial": serial, "avd": avd, "host_rss_mb": rss, "started_by_tsctl": serial in state,
                         "restart_needed": (rss or 0) > RSS_RESTART_MB})
    lines = [f"{r['serial']}\tavd={r['avd']}\tmemory={r['host_rss_mb']}MB\tstarted-by-tsctl={r['started_by_tsctl']}"
             + (f"\tRESTART (above {RSS_RESTART_MB} MB)" if r["restart_needed"] else "") for r in rows]
    return Out(rows, "\n".join(lines) or "no emulator running")


def cmd_emu_start(ctx):
    a = ctx.args
    running = [s for s in adb_serials() if classify_serial(s)[0]]
    if running:
        avd = emulator_avd(running[0])
        return Out({"serial": running[0], "avd": avd, "reused": True},
                   f"Reusing running emulator {running[0]} (avd={avd}); not starting another.")
    used = {int(s.split("-")[1]) for s in adb_serials() if s.startswith("emulator-")}
    port = next(p for p in range(5554, 5684, 2) if p not in used)
    serial = f"emulator-{port}"
    cmd = [emulator_binary(), "-avd", a.avd, "-port", str(port), "-no-audio", "-no-boot-anim",
           "-gpu", a.gpu, "-cores", "2", "-memory", "2048", "-no-snapshot-save",
           "-camera-back", "emulated", "-camera-front", "emulated"]
    if not a.window:
        cmd.append("-no-window")
    if a.cold:
        cmd.append("-no-snapshot-load")
    STATE_DIR.mkdir(parents=True, exist_ok=True)
    log_file = STATE_DIR / f"{serial}.log"
    log(f"Starting {a.avd} as {serial} (log: {log_file})")
    with open(log_file, "w") as fh:
        proc = subprocess.Popen(cmd, stdout=fh, stderr=subprocess.STDOUT, start_new_session=True)
    state = load_state()
    state[serial] = {"pid": proc.pid, "avd": a.avd, "gpu": a.gpu, "started": time.time()}
    save_state(state)
    deadline = time.time() + a.timeout
    while time.time() < deadline:
        if proc.poll() is not None:
            raise Failed(f"Emulator exited with {proc.returncode}; see {log_file}")
        if serial in adb_serials() and getprop(serial, "sys.boot_completed") == "1":
            break
        time.sleep(3)
    else:
        raise Failed(f"Emulator did not boot within {a.timeout}s; see {log_file}. Stop it with `tsctl emu stop`.")
    for setting in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
        run(["adb", "-s", serial, "shell", "settings", "put", "global", setting, "0"], check=False)
    for extra in (["svc", "power", "stayon", "true"], ["input", "keyevent", "KEYCODE_WAKEUP"], ["wm", "dismiss-keyguard"]):
        run(["adb", "-s", serial, "shell", *extra], check=False)
    return Out({"serial": serial, "avd": a.avd, "pid": proc.pid, "reused": False},
               f"ready: {serial} (avd={a.avd}, pid={proc.pid})")


def cmd_emu_stop(ctx):
    state = load_state()
    serial = ctx.device or (next(iter(state)) if len(state) == 1 else None)
    if not serial or serial not in state:
        raise Refused(f"Refused: tsctl only stops emulators it started and that are still running. "
                      f"Started by tsctl: {list(state) or 'none'}.")
    ctx.mutate(["adb", "-s", serial, "emu", "kill"], check=False)
    if ctx.dry_run:
        return Out({"serial": serial, "stopped": False})
    pid = state[serial]["pid"]
    for _ in range(30):
        if not pid_alive(pid):
            break
        time.sleep(1)
    state.pop(serial)
    save_state(state)
    return Out({"serial": serial, "stopped": True}, f"stopped {serial}")


# ---------------------------------------------------------------- commands: build and test

def cmd_build(ctx):
    variants = ctx.args.variants or [ctx.variant]
    code = gradle(ctx, [assemble_task(v) for v in variants])
    apks = {v: str(latest_apk(v)) for v in variants} if code == 0 and not ctx.dry_run else {}
    return Out({"exit_code": code, "apks": apks}, "\n".join(f"{v}: {p}" for v, p in apks.items()), code)


def cmd_lint(ctx):
    task = "lintDebug" if ctx.args.android else "detektBaseline" if ctx.args.baseline else "detekt"
    code = gradle(ctx, [task])
    return Out({"exit_code": code}, code=code)


def cmd_test_unit(ctx):
    code = gradle(ctx, ["testDebugUnitTest"] + (["--tests", ctx.args.filter, "-PmanualTests"] if ctx.args.filter else []))
    return Out({"exit_code": code, "reports": str(REPO / "app/build/reports/tests/testDebugUnitTest")}, code=code)


def cmd_test_android(ctx):
    serial = ctx.target.serial
    # clearPackageData wipes the debug app before each test and AGP uninstalls it afterwards.
    backup = backup_app_data(ctx, "test-android", package=BASE_PACKAGE)
    if backup:
        log(backup_line(backup))
    if gradle(ctx, ["assembleDebug", "assembleDebugAndroidTest"], serial=serial) != 0:
        return Out({"exit_code": 1}, code=1)
    tasks = ["connectedDebugAndroidTest"]
    if ctx.args.filter:
        tasks += [f"-Pandroid.testInstrumentationRunnerArguments.class={ctx.args.filter}", "-PmanualTests"]
    if ctx.args.shard:
        index, total = ctx.args.shard
        tasks += [f"-Pandroid.testInstrumentationRunnerArguments.numShards={total}",
                  f"-Pandroid.testInstrumentationRunnerArguments.shardIndex={index}"]
    code = gradle(ctx, tasks, serial=serial, timeout=ctx.args.timeout)
    reports = str(REPO / "app/build/reports/androidTests/connected")
    return Out({"exit_code": code, "reports": reports, "backup": str(backup) if backup else None},
               "" if ctx.dry_run else "\n".join(l for l in (f"reports: {reports}", backup_line(backup)) if l), code)


# ---------------------------------------------------------------- commands: app lifecycle

def cmd_app_install(ctx):
    variant = VARIANTS[ctx.variant]
    if not variant.installable:
        raise Failed(f"{ctx.variant} is not installable: {variant.note} Use `tsctl build {ctx.variant}`.")
    before = installed_info(ctx.target, ctx.package)
    if not ctx.args.no_build and gradle(ctx, [assemble_task(ctx.variant)]) != 0:
        return Out({"exit_code": 1}, code=1)
    apk = latest_apk(ctx.variant)
    if not apk and not ctx.dry_run:
        raise Failed(f"No APK in app/build/outputs/apk/{ctx.variant}. Drop --no-build.")
    lines = [f"will replace {ctx.package} {before.get('versionName')} (updated {before.get('lastUpdateTime')}); app data is kept"] if before else []
    result = ctx.mutate(["android", "install", f"--device={ctx.target.serial}", f"--apks={apk}"])
    if result is not None:
        log(text(result).strip())
    after = None if ctx.dry_run else installed_info(ctx.target, ctx.package)
    if after:
        lines.append(f"installed {ctx.package} {after.get('versionName')} (updated {after.get('lastUpdateTime')})")
    return Out({"before": before, "after": after, "apk": str(apk) if apk else None}, "\n".join(lines))


def cmd_app_uninstall(ctx):
    if not installed_info(ctx.target, ctx.package):
        return Out({"uninstalled": False}, f"{ctx.package} is not installed")
    backup = backup_app_data(ctx, "uninstall")
    ctx.mutate(ctx.target.adb("uninstall", ctx.package))
    lines = [f"{'will uninstall' if ctx.dry_run else 'uninstalled'} {ctx.package} and all of its data", backup_line(backup)]
    if backup:
        lines.append("restoring needs the app installed again first (`tsctl app install`)")
    return Out({"uninstalled": not ctx.dry_run, "backup": str(backup) if backup else None}, "\n".join(l for l in lines if l))


def cmd_app_clear(ctx):
    backup = backup_app_data(ctx, "clear")
    ctx.mutate(ctx.target.adb("shell", "pm", "clear", ctx.package))
    lines = [f"{'will delete' if ctx.dry_run else 'deleted'} all data of {ctx.package}", backup_line(backup)]
    return Out({"cleared": not ctx.dry_run, "backup": str(backup) if backup else None}, "\n".join(l for l in lines if l))


def cmd_app_seed(ctx):
    require_debuggable(ctx)
    backup = backup_app_data(ctx, "seed")
    if ctx.args.clear:
        ctx.mutate(ctx.target.adb("shell", "pm", "clear", ctx.package))
    if (ctx.args.clear or read_prefs(ctx) is None) and not ctx.dry_run:
        # First launch creates the prefs file and runs migrations.
        run(launch_cmd(ctx))
        for _ in range(20):
            if read_prefs(ctx) is not None:
                break
            time.sleep(1)
    changes = edit_prefs(ctx, SEED_PREFS)
    permissions = [] if ctx.args.no_permissions else set_permissions(ctx, ALL_PERMISSIONS, grant=True)
    ctx.mutate(launch_cmd(ctx))
    lines = [changes_text(changes), permissions_text(permissions), backup_line(backup)]
    return Out({"prefs": changes, "permissions": permissions, "backup": str(backup) if backup else None},
               "\n".join(l for l in lines if l))


def cmd_app_launch(ctx):
    return Out({"launched": ctx.package}, text(run(launch_cmd(ctx))).strip())


def cmd_app_stop(ctx):
    run(ctx.target.adb("shell", "am", "force-stop", ctx.package))
    return Out({"stopped": ctx.package}, f"stopped {ctx.package}")


def cmd_app_kill(ctx):
    run(ctx.target.adb("shell", "am", "kill", ctx.package))
    return Out({"killed": ctx.package}, f"killed {ctx.package} (widgets and pending intents are kept)")


def cmd_app_idle(ctx):
    pkg = ctx.package
    if ctx.args.off:
        steps = (("set-standby-bucket", pkg, "active"), ("set-inactive", pkg, "false"))
    else:
        steps = (("kill", pkg), ("set-standby-bucket", pkg, "restricted"), ("set-inactive", pkg, "true"), ("make-uid-idle", pkg))
    for step in steps:
        run(ctx.target.adb("shell", "am", *step))
    state = "active" if ctx.args.off else "idle (killed, restricted bucket, idle uid)"
    return Out({"package": pkg, "idle": not ctx.args.off}, f"{pkg} is {state}")


def cmd_app_services(ctx):
    out = text(ctx.target.shell("dumpsys", "activity", "services", ctx.package, check=False))
    services = re.findall(r"ServiceRecord\{\S+ \S+ (\S+)", out)
    return Out({"services": services}, "\n".join(services) or "no running services")


def cmd_doze(ctx):
    on = ctx.args.state == "on"
    out = text(run(ctx.target.adb("shell", "dumpsys", "deviceidle", "force-idle" if on else "unforce"))).strip().splitlines()
    if on:
        time.sleep(3)
    return Out({"doze": on}, out[0] if out else f"doze {ctx.args.state}")


def home_widgets(ctx):
    prefix = f"{ctx.package}:id/"
    return [n for n in layout_nodes(ctx) if str(n.get("resource-id", "")).startswith(prefix)]


def cmd_widget_tap(ctx):
    run(ctx.target.adb("shell", "input", "keyevent", "KEYCODE_HOME"))
    time.sleep(1)
    nodes = home_widgets(ctx)
    if not nodes:
        raise Failed(f"no {ctx.package} widget found on the home screen (add one with `widget add`)")
    x, y = (int(v) for v in nodes[0]["center"].strip("[]").split(","))
    run(ctx.target.adb("shell", "input", "tap", str(x), str(y)))
    return Out({"x": x, "y": y}, f"tapped the widget at [{x},{y}]")


def cmd_widget_add(ctx):
    label = ctx.args.label
    adb_shell = lambda *a: run(ctx.target.adb("shell", *a))
    size = re.search(r"(\d+)x(\d+)", text(ctx.target.shell("wm", "size")))
    w, h = (int(size.group(1)), int(size.group(2))) if size else (720, 1280)
    adb_shell("input", "keyevent", "KEYCODE_HOME")
    time.sleep(1)
    adb_shell("input", "swipe", str(w // 2), str(h // 2), str(w // 2), str(h // 2), "1500")
    android_input(ctx, "input tap", "--wait=5", "text=Widgets")
    android_input(ctx, "input scroll", "--dir=down", "--until=text=Trail Sense", "scrollable=true")
    android_input(ctx, "input tap", "text=Trail Sense")
    time.sleep(1)
    android_input(ctx, "input scroll", "--dir=down", f"--until=text=/^{re.escape(label)}$/", "scrollable=true")
    cell = next((n for n in layout_nodes(ctx) if str(n.get("content-desc", "")).startswith(f"{label} widget")
                 and "WidgetCell" in str(n.get("class", ""))), None)
    if not cell:
        raise Failed(f"no widget named '{label}' under Trail Sense in the launcher picker")
    cx, cy = cell["center"].strip("[]").split(",")
    adb_shell("input", "tap", cx, cy)
    time.sleep(1)
    add = next((n for n in layout_nodes(ctx) if str(n.get("resource-id", "")).endswith(":id/widget_add_button")), None)
    if not add:
        raise Failed("the launcher did not show an Add button")
    ax, ay = add["center"].strip("[]").split(",")
    adb_shell("input", "tap", ax, ay)
    time.sleep(2)
    save = next((n for n in layout_nodes(ctx) if str(n.get("resource-id", "")).endswith(":id/button_save")), None)
    if save:
        sx, sy = save["center"].strip("[]").split(",")
        adb_shell("input", "tap", sx, sy)
        time.sleep(2)
    if not home_widgets(ctx):
        raise Failed("the widget did not appear on the home screen")
    return Out({"widget": label}, f"added the {label} widget to the home screen")


def cmd_app_current(ctx):
    out = text(ctx.target.shell("dumpsys", "activity", "activities", check=False))
    m = re.search(r"topResumedActivity=ActivityRecord\{\S+ \S+ (\S+)", out) or re.search(r"mResumedActivity: ActivityRecord\{\S+ \S+ (\S+)", out)
    activity = m.group(1) if m else None
    return Out({"activity": activity}, activity or "no resumed activity")


def cmd_tools(ctx):
    ids = sorted(tool_ids().items(), key=lambda kv: kv[1])
    rows = [{"id": i, "name": n} for n, i in ids if not ctx.args.filter or ctx.args.filter.lower() in n]
    return Out(rows, "\n".join(f"{r['id']}\t{r['name']}" for r in rows))


def cmd_open(ctx):
    tool_id = resolve_tool(ctx.args.tool, tool_ids())
    # tool_id must be a long extra: MainActivity reads it with getLongExtra.
    out = text(run(launch_cmd(ctx, ["-a", OPEN_TOOL_ACTION, "--el", "tool_id", str(tool_id)])))
    if "Error" in out:
        raise Failed(out.strip())
    return Out({"tool_id": tool_id}, f"opened tool {tool_id}")


# ---------------------------------------------------------------- commands: environment

def set_permissions(ctx, names, grant):
    results = []
    for name in names:
        if name in APPOP_ALIASES:
            mode = "allow" if grant else "deny"
            ctx.mutate(ctx.target.adb("shell", "appops", "set", ctx.package, APPOP_ALIASES[name], mode))
            results.append({"permission": "appop:" + APPOP_ALIASES[name], "result": mode})
            continue
        for perm in PERMISSION_ALIASES.get(name, [name if "." in name else "android.permission." + name.upper()]):
            result = ctx.mutate(ctx.target.adb("shell", "pm", "grant" if grant else "revoke", ctx.package, perm), check=False)
            if result is not None and result.returncode != 0:
                results.append({"permission": perm, "result": "failed", "error": (result.stderr or result.stdout).decode().strip()})
            else:
                results.append({"permission": perm, "result": "granted" if grant else "revoked"})
    return results


def permissions_text(results):
    return "\n".join(f"{r['permission']}: {r['result']}" + (f" ({r['error']})" if r.get("error") else "") for r in results)


def cmd_perm_set(ctx):
    names = ALL_PERMISSIONS if "all" in ctx.args.names else ctx.args.names
    grant = ctx.args.perm_cmd == "grant"
    if not grant:
        log("note: revoking a runtime permission kills the app process")
    results = set_permissions(ctx, names, grant)
    return Out(results, permissions_text(results), 1 if any(r["result"] == "failed" for r in results) else 0)


def cmd_perm_list(ctx):
    granted = parse_granted_permissions(text(ctx.target.shell("dumpsys", "package", ctx.package)))
    appop = text(ctx.target.shell("appops", "get", ctx.package, "SCHEDULE_EXACT_ALARM", check=False)).strip()
    data = {"permissions": granted, "appops": {"SCHEDULE_EXACT_ALARM": appop.split(": ")[-1] if appop else None}}
    lines = [f"{p}\t{g}" for p, g in sorted(granted.items())] + [f"appop SCHEDULE_EXACT_ALARM\t{data['appops']['SCHEDULE_EXACT_ALARM']}"]
    return Out(data, "\n".join(lines))


def cmd_geo(ctx):
    a = ctx.args
    cmd = ctx.target.adb("emu", "geo", "fix", str(a.lon), str(a.lat)) + ([str(a.alt)] if a.alt is not None else [])
    run(cmd)
    return Out({"lat": a.lat, "lon": a.lon, "alt": a.alt},
               f"location set to lat={a.lat} lon={a.lon}" + (f" alt={a.alt}" if a.alt is not None else ""))


def cmd_sensor_set(ctx):
    if ctx.args.name not in EMULATOR_SENSORS:
        raise UsageError(f"Unknown sensor {ctx.args.name}. Known: {', '.join(EMULATOR_SENSORS)}")
    run(ctx.target.adb("emu", "sensor", "set", ctx.args.name, ":".join(ctx.args.values)))
    return Out({"sensor": ctx.args.name, "values": ctx.args.values}, f"{ctx.args.name} = {':'.join(ctx.args.values)}")


def cmd_sensor_get(ctx):
    out = text(run(ctx.target.adb("emu", "sensor", "get", ctx.args.name))).splitlines()[0].strip()
    return Out({"sensor": ctx.args.name, "value": out.split(" = ")[-1]}, out)


def cmd_sensor_status(ctx):
    lines = [l.strip() for l in text(run(ctx.target.adb("emu", "sensor", "status"))).splitlines() if ":" in l]
    data = {l.split(":")[0]: l.split(":", 1)[1].strip().rstrip(".") for l in lines}
    return Out(data, "\n".join(lines))


# ---------------------------------------------------------------- commands: prefs and db

def cmd_pref_keys(ctx):
    rows = [{"ref": "@" + n, "key": v} for n, v in sorted(string_resources().items())
            if n.startswith("pref_") and (not ctx.args.filter or ctx.args.filter.lower() in n.lower())]
    return Out(rows, "\n".join(f"{r['ref']}\t{r['key']}" for r in rows))


def read_prefs_or_fail(ctx):
    require_debuggable(ctx)
    root = read_prefs(ctx)
    if root is None:
        raise Failed(f"{prefs_path(ctx)} does not exist yet. Run `tsctl app launch` once.")
    return prefs_dict(root)


def cmd_pref_get(ctx):
    key = resolve_pref_key(ctx.args.key, string_resources())
    entry = read_prefs_or_fail(ctx).get(key)
    if entry is None:
        return Out({"key": key, "value": None}, f"{key}\t<unset>", 1)
    return Out({"key": key, **entry}, f"{key}\t{json.dumps(entry['value'])}\t{entry['type']}")


def cmd_pref_list(ctx):
    prefs = {k: v for k, v in sorted(read_prefs_or_fail(ctx).items())
             if not ctx.args.filter or ctx.args.filter.lower() in k.lower()}
    return Out(prefs, "\n".join(f"{k}\t{json.dumps(v['value'])}\t{v['type']}" for k, v in prefs.items()))


def cmd_pref_edit(ctx, edit, reason):
    require_debuggable(ctx)
    backup = backup_app_data(ctx, reason)
    changes = edit_prefs(ctx, [edit])
    lines = [changes_text(changes), backup_line(backup),
             "" if ctx.dry_run else "app was force-stopped; the change applies on next launch"]
    return Out({"changes": changes, "backup": str(backup) if backup else None}, "\n".join(l for l in lines if l))


def cmd_pref_set(ctx):
    return cmd_pref_edit(ctx, (ctx.args.key, ctx.args.type, ctx.args.value), "pref-set")


def cmd_pref_delete(ctx):
    return cmd_pref_edit(ctx, (ctx.args.key, None, None), "pref-delete")


def with_db(ctx, fn):
    require_debuggable(ctx)
    with tempfile.TemporaryDirectory(prefix="tsctl-db-") as tmp:
        return fn(pull_db(ctx, Path(tmp)), Path(tmp))


def cmd_db_tables(ctx):
    rows = with_db(ctx, lambda db, _: sqlite_rows(db, "select name from sqlite_master where type='table' order by name"))
    names = [r["name"] for r in rows]
    return Out(names, "\n".join(names))


def cmd_db_schema(ctx):
    sql = with_db(ctx, lambda db, _: sqlite(db, f".schema {ctx.args.table}" if ctx.args.table else ".schema"))
    return Out({"schema": sql}, sql.strip())


def cmd_db_query(ctx):
    rows = with_db(ctx, lambda db, _: sqlite_rows(db, ctx.args.sql))
    table = with_db(ctx, lambda db, _: sqlite(db, ctx.args.sql)) if not ctx.json else ""
    return Out(rows, table.rstrip())


def cmd_db_pull(ctx):
    require_debuggable(ctx)
    path = pull_db(ctx, Path(ctx.args.dest).resolve())
    return Out({"path": str(path)}, f"pulled {path}")


def cmd_db_exec(ctx):
    require_debuggable(ctx)
    # Also stops the app, so it can't append to the WAL while we copy and rewrite the file.
    backup = backup_app_data(ctx, "db-exec")
    force_stop(ctx)

    def apply(db, tmp):
        if ctx.dry_run:
            changed = sqlite_rows(db, f"{ctx.args.sql}; SELECT changes() AS rows_changed;")
            ctx.plan(f"push the result to {ctx.package}:databases/{DATABASE} (dry run applied to a scratch copy only)")
            return {"rows_changed": changed[-1]["rows_changed"] if changed else 0, "backup": None}
        changed = sqlite_rows(db, f"{ctx.args.sql}; SELECT changes() AS rows_changed;")
        sqlite(db, "PRAGMA wal_checkpoint(TRUNCATE);")
        write_app_file(ctx, f"databases/{DATABASE}", db.read_bytes())
        # A stale WAL on the device would replay over the file we just pushed.
        run_as(ctx, "rm", "-f", f"databases/{DATABASE}-wal", f"databases/{DATABASE}-shm")
        return {"rows_changed": changed[-1]["rows_changed"] if changed else 0, "backup": str(backup) if backup else None}

    result = with_db(ctx, apply)
    lines = [f"rows changed: {result['rows_changed']}"]
    if result["backup"]:
        lines += [backup_line(result["backup"]), "app was force-stopped; relaunch to see the change"]
    return Out(result, "\n".join(lines))


def cmd_app_restore(ctx):
    """Puts back the prefs and database from a backup directory, replacing what is there now."""
    src = Path(ctx.args.backup)
    try:
        manifest = json.loads((src / "manifest.json").read_text())
    except (OSError, ValueError):
        raise UsageError(f"{src} has no manifest.json; pass a directory printed as `backup:` by tsctl.")
    if manifest["package"] != ctx.package:
        raise UsageError(f"The backup is of {manifest['package']}; rerun with the matching --variant.")
    require_debuggable(ctx)
    current = backup_app_data(ctx, "before-restore")
    force_stop(ctx)
    restored = []
    for rel in manifest["files"]:
        if rel.startswith("shared_prefs/"):
            write_app_file(ctx, rel, (src / rel).read_bytes())
            restored.append(rel)
    db = src / "databases" / DATABASE
    if db.exists():
        with tempfile.TemporaryDirectory(prefix="tsctl-restore-") as tmp:
            for f in (src / "databases").iterdir():
                shutil.copy(f, Path(tmp) / f.name)
            # Fold the backed-up WAL into the main file; the device's own WAL and SHM are removed below.
            sqlite(Path(tmp) / DATABASE, "PRAGMA wal_checkpoint(TRUNCATE);")
            write_app_file(ctx, f"databases/{DATABASE}", (Path(tmp) / DATABASE).read_bytes())
        ctx.mutate(ctx.target.adb("shell", "run-as", ctx.package, "rm", "-f", f"databases/{DATABASE}-wal", f"databases/{DATABASE}-shm"))
        restored.append(f"databases/{DATABASE}")
    lines = [f"{'will restore' if ctx.dry_run else 'restored'} {len(restored)} files from {src}", backup_line(current)]
    return Out({"restored": restored, "done": not ctx.dry_run, "backup": str(current) if current else None},
               "\n".join(l for l in lines if l))


# ---------------------------------------------------------------- commands: ui and evidence

def android_cmd(ctx, sub, *rest):
    return ["android", *sub.split(), f"--device={ctx.target.serial}", *rest]


def android_input(ctx, sub, *rest):
    result = run(android_cmd(ctx, sub, *rest), check=False)
    results = parse_input_results(text(result))
    if results is None:
        raise Failed((text(result) + result.stderr.decode(errors="replace")).strip() or f"android {sub} failed")
    lines = []
    for r in results:
        if r.get("status") == "success":
            lines.append(f"ok {r.get('action')}: " + (describe_node(r["target"]) if r.get("target") else ""))
        else:
            lines.append(f"FAILED {r.get('action')}: {r.get('status')} {r.get('error', '')}")
            lines += ["  match: " + describe_node(m) for m in r.get("matches", [])]
    if result.returncode != 0:
        lines.append("hint: tighten the selector (id=/:id\\/name$/, text=/^Exact$/) or pass --first")
    return Out(results, "\n".join(lines), result.returncode)


def layout_output(ctx, *flags):
    """Fetch a layout, falling back to --no-idle for screens that never stop updating (e.g. Sensors)."""
    result = run(android_cmd(ctx, "layout", *flags), check=False)
    if result.returncode != 0 and b"Could not obtain idle state" in result.stdout + result.stderr:
        result = run(android_cmd(ctx, "layout", "--no-idle", *flags), check=False)
    if result.returncode != 0:
        raise Failed((text(result) + result.stderr.decode(errors="replace")).strip() or "android layout failed")
    return text(result)


def layout_nodes(ctx):
    return flatten_layout(json.loads(layout_output(ctx, "--flat")))


def short_node(n, keys=("text", "content-desc", "resource-id", "class", "center", "interactions", "state", "off-screen")):
    return {k: n[k] for k in keys if k in n}


def cmd_ui_layout(ctx):
    data = json.loads(layout_output(ctx, *(["--full"] if ctx.args.full else [])))
    if ctx.args.out:
        Path(ctx.args.out).write_text(json.dumps(data, indent=2))
        return Out({"path": ctx.args.out}, f"wrote {ctx.args.out}")
    return Out(data, json.dumps(data, indent=2))


def cmd_ui_find(ctx):
    hits = [short_node(n) for n in layout_nodes(ctx) if node_matches(n, ctx.args.text)]
    return Out(hits, "\n".join(json.dumps(h) for h in hits) or f"nothing contains '{ctx.args.text}'", 0 if hits else 1)


def cmd_ui_wait(ctx):
    deadline = time.time() + ctx.args.timeout
    while time.time() < deadline:
        hit = next((n for n in layout_nodes(ctx) if node_matches(n, ctx.args.text)), None)
        if hit:
            return Out(short_node(hit), json.dumps(short_node(hit, ("text", "content-desc", "resource-id", "center"))))
        time.sleep(1)
    return Out(None, f"timed out after {ctx.args.timeout}s waiting for '{ctx.args.text}'", 1)


def input_flags(a):
    return ([f"--wait={a.wait}"] if getattr(a, "wait", None) else []) + (["--match=first"] if getattr(a, "first", False) else [])


def cmd_ui_tap(ctx):
    return android_input(ctx, "input tap", *input_flags(ctx.args), ctx.args.selector)


def cmd_ui_tapxy(ctx):
    run(ctx.target.adb("shell", "input", "tap", str(ctx.args.x), str(ctx.args.y)))
    return Out({"x": ctx.args.x, "y": ctx.args.y}, f"ok tap at [{ctx.args.x},{ctx.args.y}]")


def cmd_ui_hold(ctx):
    x, y = ctx.args.x, ctx.args.y
    run(ctx.target.adb("shell", "input", "swipe", str(x), str(y), str(x), str(y), str(ctx.args.ms)))
    return Out({"x": x, "y": y, "ms": ctx.args.ms}, f"ok long press at [{x},{y}] for {ctx.args.ms} ms")


def cmd_ui_type(ctx):
    a = ctx.args
    return android_input(ctx, "input text", *input_flags(a), *(["--submit"] if a.submit else []), a.text, a.selector)


def cmd_ui_scroll(ctx):
    a = ctx.args
    return android_input(ctx, "input scroll", f"--dir={a.dir}", *([f"--until={a.until}"] if a.until else []), *input_flags(a), a.selector)


def cmd_ui_swipe(ctx):
    return android_input(ctx, "input swipe", f"--dir={ctx.args.dir}", *input_flags(ctx.args), ctx.args.selector)


def cmd_ui_key(ctx):
    return android_input(ctx, "input key", ctx.args.key)


def cmd_ui_shot(ctx):
    Path(ctx.args.path).parent.mkdir(parents=True, exist_ok=True)
    run(android_cmd(ctx, "screen capture", *(["--annotate"] if ctx.args.annotate else []), f"--output={ctx.args.path}"))
    return Out({"path": ctx.args.path}, f"wrote {ctx.args.path}")


def run_dir():
    return CAPTURES / (os.environ.get("TSCTL_RUN") or datetime.date.today().isoformat())


def cmd_ui_snap(ctx):
    base = run_dir() / ctx.args.name
    base.parent.mkdir(parents=True, exist_ok=True)
    run(android_cmd(ctx, "screen capture", f"--output={base}.png"))
    Path(f"{base}.layout.json").write_text(layout_output(ctx, "--pretty"))
    return Out({"png": f"{base}.png", "layout": f"{base}.layout.json"}, f"wrote {base}.png and {base}.layout.json")


RECORD_STATE = "/sdcard/tsctl-record.txt"


def cmd_ui_record(ctx):
    """Record the screen on the device (screenrecord, capped at 180 s) and pull the mp4 on stop."""
    sh = ctx.target.shell
    if ctx.args.action == "start":
        if text(sh("cat", RECORD_STATE, check=False)).strip():
            raise UsageError("a recording is already running; run `tsctl ui record stop` first")
        remote = f"/sdcard/tsctl-{os.getpid()}.mp4"
        script = f"screenrecord --time-limit 180 {remote} >/dev/null 2>&1 & echo $!"
        pid = text(sh("sh", "-c", shlex.quote(script))).strip()
        sh("sh", "-c", shlex.quote(f"echo {pid} {remote} {shlex.quote(ctx.args.name)} > {RECORD_STATE}"))
        return Out({"pid": pid}, f"recording to {ctx.args.name}.mp4 (stop with `tsctl ui record stop`; auto-stops after 180 s)")
    state = text(sh("cat", RECORD_STATE, check=False)).split()
    if len(state) != 3:
        raise UsageError("no recording is running")
    pid, remote, name = state
    sh("kill", "-2", pid, check=False)
    for _ in range(20):
        if not text(sh("ls", f"/proc/{pid}", check=False)).strip():
            break
        time.sleep(0.5)
    dest = run_dir() / f"{name}.mp4"
    dest.parent.mkdir(parents=True, exist_ok=True)
    run(ctx.target.adb("pull", remote, str(dest)))
    sh("rm", "-f", remote, RECORD_STATE)
    return Out({"mp4": str(dest)}, f"wrote {dest}")


def cmd_logs(ctx):
    a = ctx.args
    if a.clear:
        run(ctx.target.adb("logcat", "-c"))
        return Out({"cleared": True}, "logcat cleared")
    if a.crash:
        out = text(run(ctx.target.adb("logcat", "-d", "-b", "crash")))
    else:
        pid = text(ctx.target.shell("pidof", ctx.package, check=False)).strip()
        cmd = ctx.target.adb("logcat", "-d", "-t", str(a.lines))
        if pid and not a.all:
            cmd.append(f"--pid={pid.split()[0]}")
        elif not a.all:
            log(f"{ctx.package} is not running; showing all logs")
        out = text(run(cmd))
    if a.grep:
        pattern = re.compile(a.grep)
        out = "".join(l + "\n" for l in out.splitlines() if pattern.search(l))
    if a.out:
        Path(a.out).parent.mkdir(parents=True, exist_ok=True)
        Path(a.out).write_text(out)
        return Out({"path": a.out, "lines": len(out.splitlines())}, f"wrote {a.out} ({len(out.splitlines())} lines)")
    return Out({"lines": out.splitlines()}, out.rstrip())


DISPLAY_SETTINGS = ("size", "density", "font-scale", "dark", "rotation")


def cmd_display(ctx):
    """Change screen size, density, font scale, dark mode, or rotation to check responsive layouts."""
    a = ctx.args
    sh = ctx.target.shell
    if a.setting == "reset":
        for cmd in (("wm", "size", "reset"), ("wm", "density", "reset"),
                    ("settings", "put", "system", "font_scale", "1.0"),
                    ("cmd", "uimode", "night", "no"),
                    ("settings", "put", "system", "accelerometer_rotation", "1")):
            sh(*cmd)
        return Out({"reset": True}, "display reset (size, density, font scale, dark mode, rotation)")
    if a.setting == "status":
        data = {
            "size": text(sh("wm", "size")).strip(),
            "density": text(sh("wm", "density")).strip(),
            "font-scale": text(sh("settings", "get", "system", "font_scale")).strip(),
            "dark": text(sh("cmd", "uimode", "night")).strip(),
            "rotation": text(sh("settings", "get", "system", "user_rotation")).strip(),
        }
        return Out(data, "\n".join(f"{k}: {v}" for k, v in data.items()))
    if a.value is None:
        raise UsageError(f"usage: tsctl env display {a.setting} VALUE")
    commands = {
        "size": ("wm", "size", a.value),
        "density": ("wm", "density", a.value),
        "font-scale": ("settings", "put", "system", "font_scale", a.value),
        "dark": ("cmd", "uimode", "night", "yes" if a.value in ("on", "yes", "true") else "no"),
    }
    if a.setting == "rotation":
        if a.value not in ("0", "1", "2", "3"):
            raise UsageError("rotation must be 0, 1, 2 or 3 (quarter turns)")
        sh("settings", "put", "system", "accelerometer_rotation", "0")
        sh("settings", "put", "system", "user_rotation", a.value)
    else:
        sh(*commands[a.setting])
    return Out({a.setting: a.value}, f"{a.setting} = {a.value} (undo with `tsctl env display reset`)")


def cmd_notifications(ctx):
    out = text(ctx.target.shell("dumpsys", "notification", "--noredact", check=False))
    records = re.split(r"(?m)^\s*NotificationRecord\(", out)[1:]
    mine = [r.splitlines()[0] + " " + " ".join(re.findall(r"android\.(?:title|text)=\S.*", r)) for r in records if f"pkg={ctx.package}" in r]
    return Out({"notifications": mine}, "\n".join(mine) or f"no notifications from {ctx.package}")


def cmd_adb(ctx):
    rest = ctx.args.rest[1:] if ctx.args.rest[:1] == ["--"] else ctx.args.rest
    if not rest:
        raise UsageError("usage: tsctl adb -- <adb args>")
    if ctx.mutate(ctx.target.adb(*rest), check=False) is None:
        return Out({"dry_run": True})
    result = run(ctx.target.adb(*rest), check=False)
    out = text(result) + result.stderr.decode(errors="replace")
    return Out({"exit_code": result.returncode, "output": out}, out.rstrip(), result.returncode)


# ---------------------------------------------------------------- argparse

READ = Policy(risk="act")
ACT = Policy(risk="act")
APP_DATA = Policy(risk="act", app_data=True)
DESTRUCTIVE = Policy(risk="destructive", dry_run=True)
DESTRUCTIVE_APP_DATA = Policy(risk="destructive", dry_run=True, app_data=True)
EMULATOR = Policy(risk="act", emulator_only=True)
UI = Policy(risk="act", focus_check=True)

EPILOG = """examples:
  tsctl emu start && tsctl app install && tsctl app seed --clear && tsctl doctor
  tsctl open beacons
  tsctl ui tap id=create_btn ; tsctl ui type "Camp" 'id=/:id\\/text_input$/'
  tsctl env geo 42.36 -71.06 20 ; tsctl env sensor set pressure 990.5
  tsctl db query "select name from beacons" --json
  tsctl --dry-run app clear ; tsctl test unit '*BeaconNavigatorTest'

global options work before or after the command. Every command accepts --json.
exit codes: 0 ok, 1 failed, 2 usage, 3 refused by a safety gate."""


def build_parser():
    common = argparse.ArgumentParser(add_help=False)
    g = common.add_argument_group("global options")
    g.add_argument("--device", default=argparse.SUPPRESS, help="adb serial; default is the single running emulator")
    g.add_argument("--variant", default=argparse.SUPPRESS, choices=list(VARIANTS), help="package to act on (default debug)")
    g.add_argument("--dry-run", action="store_true", default=argparse.SUPPRESS,
                   help="show what a destructive command would do; rejected by commands without a dry-run mode")
    g.add_argument("--user-approved", metavar="SERIAL", default=argparse.SUPPRESS,
                   help="one-off approval for an action on a physical device; pass only after the user approved that action")
    g.add_argument("--json", action="store_true", default=argparse.SUPPRESS, help="machine-readable output on stdout")

    root = argparse.ArgumentParser(prog="tsctl", parents=[common], epilog=EPILOG,
                                   formatter_class=argparse.RawDescriptionHelpFormatter,
                                   description="Drive Trail Sense on an emulator (or, with approval, a device) for verification.")
    top = root.add_subparsers(dest="command", required=True, metavar="<command>")

    def group(name, help_text, epilog=None, parent=top):
        p = parent.add_parser(name, parents=[common], help=help_text, description=help_text, epilog=epilog,
                              formatter_class=argparse.RawDescriptionHelpFormatter)
        return p.add_subparsers(dest=f"{name}_cmd", required=True, metavar="<subcommand>")

    def leaf(parent, name, fn, policy, help_text, epilog=None):
        p = parent.add_parser(name, parents=[common], help=help_text, description=help_text, epilog=epilog,
                              formatter_class=argparse.RawDescriptionHelpFormatter)
        p.set_defaults(fn=fn, policy=policy)
        return p

    leaf(top, "devices", cmd_devices, NO_DEVICE, "list devices and whether each counts as emulator or PHYSICAL")
    leaf(top, "doctor", cmd_doctor, Policy(risk="inspect"),
         "readiness check: target, boot, emulator memory, installed build, onboarding, focused window (exit 1 if not ready)")

    dev = group("grants", "full-control grants for a package on a physical device",
                "Add a grant only after the user says you have full control of that package on that device.\n"
                "example: tsctl grants add R5CT123ABC com.kylecorry.trail_sense.staging")
    for name, fn, help_text in (("add", cmd_device_grant, "allow every tsctl command on PACKAGE on SERIAL without asking again"),
                                ("remove", cmd_device_revoke, "remove a grant")):
        dg = leaf(dev, name, fn, Policy(device=False, risk="act"), help_text)
        dg.add_argument("serial")
        dg.add_argument("package")
    leaf(dev, "list", cmd_device_grants, NO_DEVICE, "list grants")

    emu = group("emu", "manage the emulator (2 cores, 2 GB; reuses a running one)")
    s = leaf(emu, "start", cmd_emu_start, Policy(device=False, risk="act"), "boot an AVD headless and wait until usable, or reuse a running emulator")
    s.add_argument("--avd", default=DEFAULT_AVD, help=f"default {DEFAULT_AVD}; see `tsctl emu list`")
    s.add_argument("--gpu", default=DEFAULT_GPU, choices=["host", "swiftshader_indirect", "swangle_indirect"],
                   help=f"renderer (default {DEFAULT_GPU})")
    s.add_argument("--window", action="store_true", help="show the emulator window")
    s.add_argument("--cold", action="store_true", help="ignore the saved snapshot")
    s.add_argument("--timeout", type=positive_int, default=300, help="seconds to wait for boot")
    leaf(emu, "stop", cmd_emu_stop, NO_DEVICE_DRY, "stop the emulator tsctl started (refuses others)")
    leaf(emu, "status", cmd_emu_status, NO_DEVICE, "running emulators, their host memory, and whether tsctl started them")
    leaf(emu, "list", cmd_emu_list, NO_DEVICE, "available AVDs")

    b = leaf(top, "build", cmd_build, NO_DEVICE_DRY, "assemble variants",
             "variants: " + ", ".join(f"{k} ({v.package})" for k, v in VARIANTS.items()))
    b.add_argument("variants", nargs="*", choices=list(VARIANTS) + [[]], metavar="variant", help="default: --variant")
    ln = leaf(top, "lint", cmd_lint, NO_DEVICE_DRY, "detekt, Android lint with --android, or regenerate the detekt baseline with --baseline")
    ln.add_argument("--android", action="store_true")
    ln.add_argument("--baseline", action="store_true", help="regenerate app/detekt-baseline.xml")

    test = group("test", "unit tests on the JVM or instrumented tests on the target")
    tu = leaf(test, "unit", cmd_test_unit, NO_DEVICE_DRY, "JVM unit tests", "example: tsctl test unit '*BeaconNavigatorTest'")
    tu.add_argument("filter", nargs="?", help="Gradle --tests pattern")
    ta = leaf(test, "android", cmd_test_android, DESTRUCTIVE,
              "instrumented tests (destructive: clearPackageData wipes the debug app and AGP uninstalls it; its prefs and db are backed up first)",
              "example: tsctl test android com.kylecorry.trail_sense.tools.beacons.ToolBeaconsTest --timeout 600")
    ta.add_argument("filter", nargs="?", help="class or class#method")
    ta.add_argument("--timeout", type=positive_int, default=1800, help="seconds")
    ta.add_argument("--shard", type=shard, metavar="INDEX/TOTAL")

    app = group("app", "install and manage the app package (--variant picks which)")
    i = leaf(app, "install", cmd_app_install, DESTRUCTIVE, "build (unless --no-build) and install; replaces the installed package")
    i.add_argument("--no-build", action="store_true")
    leaf(app, "uninstall", cmd_app_uninstall, DESTRUCTIVE, "back up prefs and db, then uninstall and delete all app data")
    leaf(app, "clear", cmd_app_clear, DESTRUCTIVE, "back up prefs and db, then delete all app data (pm clear)")
    ar = leaf(app, "restore", cmd_app_restore, DESTRUCTIVE_APP_DATA,
              "put back prefs and db from a backup (the current data is backed up first)",
              "Backups are made by app clear/uninstall/seed, pref set/delete, db exec, and test android,\n"
              "under captures/verify/backups/<time>-<reason>/. Only prefs and the database are covered.")
    ar.add_argument("backup", help="a directory printed as `backup:`")
    sd = leaf(app, "seed", cmd_app_seed, DESTRUCTIVE_APP_DATA,
              "back up prefs and db, then make the app usable: onboarding done, test units, GPS altimeter, permissions granted, launched")
    sd.add_argument("--clear", action="store_true", help="pm clear first for an empty data dir")
    sd.add_argument("--no-permissions", action="store_true")
    leaf(app, "launch", cmd_app_launch, ACT, "start MainActivity")
    leaf(app, "stop", cmd_app_stop, ACT, "force-stop the app")
    leaf(app, "kill", cmd_app_kill, ACT, "kill the process (am kill) but keep widgets and pending intents; use instead of stop for background-start tests")
    ai = leaf(app, "idle", cmd_app_idle, ACT,
              "make the app idle: kill, restricted standby bucket, idle uid (the state background-start limits apply to)",
              "Pair with `tsctl env doze on`. Never use `app stop` first: force-stop cancels widget PendingIntents.")
    ai.add_argument("--off", action="store_true", help="return the app to the active standby bucket")
    leaf(app, "services", cmd_app_services, ACT, "running services of the app")
    leaf(app, "current", cmd_app_current, ACT, "the resumed activity")

    env = group("env", "fake the environment on the emulator: GPS, sensors, display, Doze (emulator only)")
    dz = leaf(env, "doze", cmd_doze, EMULATOR, "force the device into deep Doze (on) or release it (off); waits 3 s after on")
    dz.add_argument("state", choices=["on", "off"])

    widget = group("widget", "home-screen widgets (emulator launcher flow for add)")
    wa = leaf(widget, "add", cmd_widget_add, EMULATOR, "add a Trail Sense widget through the launcher picker (and save its config screen if shown)",
              "example: tsctl widget add Flashlight")
    wa.add_argument("label", help="widget name as shown in the picker, e.g. Flashlight")
    leaf(widget, "tap", cmd_widget_tap, ACT, "go home and tap the Trail Sense widget on it")

    t = leaf(top, "tools", cmd_tools, NO_DEVICE, "tool names and ids accepted by `open` (parsed from Tools.kt)")
    t.add_argument("filter", nargs="?")
    o = leaf(top, "open", cmd_open, ACT, "open a tool by name or id through the OPEN_TOOL intent (a precondition shortcut, not the user path)")
    o.add_argument("tool")

    perm = group("perm", "runtime permissions; aliases: " + ", ".join(ALL_PERMISSIONS) + ", all")
    for name, help_text in (("grant", "grant permissions"), ("revoke", "revoke permissions (kills the app)")):
        pp = leaf(perm, name, cmd_perm_set, Policy(risk="act", dry_run=True), help_text)
        pp.add_argument("names", nargs="+", help="alias, all, or android.permission.* name")
    leaf(perm, "list", cmd_perm_list, ACT, "granted state of each permission and the exact-alarm appop")

    geo = leaf(env, "geo", cmd_geo, EMULATOR, "GPS fix, latitude first", "example: tsctl env geo 42.3601 -71.0589 20")
    geo.add_argument("lat", type=float)
    geo.add_argument("lon", type=float)
    geo.add_argument("alt", type=float, nargs="?", help="meters above the ellipsoid")

    sensor = group("sensor", "virtual sensors", parent=env)
    ss = leaf(sensor, "set", cmd_sensor_set, EMULATOR, "set a sensor value",
              "examples: tsctl env sensor set pressure 990.5 ; tsctl env sensor set magnetic-field 22 5 -40\nsensors: " + ", ".join(EMULATOR_SENSORS))
    ss.add_argument("name")
    ss.add_argument("values", nargs="+")
    sg = leaf(sensor, "get", cmd_sensor_get, EMULATOR, "read a sensor value")
    sg.add_argument("name")
    leaf(sensor, "status", cmd_sensor_status, EMULATOR, "which sensors are enabled")

    pref = group("pref", "shared preferences of debuggable variants. KEY is raw or @string_resource_name",
                 "example: tsctl pref set @pref_use_24_hour true")
    pk = leaf(pref, "keys", cmd_pref_keys, NO_DEVICE, "search pref_* string resources for keys")
    pk.add_argument("filter", nargs="?")
    pg = leaf(pref, "get", cmd_pref_get, APP_DATA, "read one preference (exit 1 when unset)")
    pg.add_argument("key")
    pl = leaf(pref, "list", cmd_pref_list, APP_DATA, "read all preferences")
    pl.add_argument("filter", nargs="?")
    ps = leaf(pref, "set", cmd_pref_set, DESTRUCTIVE_APP_DATA, "force-stop the app, then write one preference")
    ps.add_argument("key")
    ps.add_argument("value")
    ps.add_argument("--type", choices=["bool", "int", "long", "float", "string"], help="default: the existing type, else string")
    pd = leaf(pref, "delete", cmd_pref_delete, DESTRUCTIVE_APP_DATA, "force-stop the app, then remove one preference")
    pd.add_argument("key")

    db = group("db", f"the Room database `{DATABASE}` of debuggable variants",
               "example: tsctl db query \"select _id, name from beacons\" --json")
    leaf(db, "tables", cmd_db_tables, APP_DATA, "table names")
    dsc = leaf(db, "schema", cmd_db_schema, APP_DATA, "CREATE statements")
    dsc.add_argument("table", nargs="?")
    dq = leaf(db, "query", cmd_db_query, APP_DATA, "read-only SQL against a pulled copy (the app keeps running)")
    dq.add_argument("sql")
    de = leaf(db, "exec", cmd_db_exec, DESTRUCTIVE_APP_DATA, "write SQL: back up prefs and db, force-stop, push")
    de.add_argument("sql")
    dp = leaf(db, "pull", cmd_db_pull, APP_DATA, "copy the database files to a local directory")
    dp.add_argument("dest")

    ui = group("ui", "drive the screen with android CLI selectors: text=, id=, desc=, class=, clickable=true; /regex/ for exact",
               "values are case-insensitive substrings; text=/^OK$/ matches exactly; an ambiguous selector fails and lists matches")
    ul = leaf(ui, "layout", cmd_ui_layout, UI, "full layout JSON")
    ul.add_argument("--full", action="store_true", help="include non-interactive and hidden elements")
    ul.add_argument("--out")
    uf = leaf(ui, "find", cmd_ui_find, UI, "elements whose text, description, or id contains TEXT (exit 1 if none)")
    uf.add_argument("text")
    uw = leaf(ui, "wait", cmd_ui_wait, UI, "poll until an element containing TEXT appears (exit 1 on timeout)")
    uw.add_argument("text")
    uw.add_argument("--timeout", type=positive_int, default=15)
    ut = leaf(ui, "tap", cmd_ui_tap, UI, "tap the element matching SELECTOR")
    ut.add_argument("selector")
    uty = leaf(ui, "type", cmd_ui_type, UI, "replace the text of the field matching SELECTOR")
    uty.add_argument("text")
    uty.add_argument("selector")
    uty.add_argument("--submit", action="store_true", help="press enter afterwards")
    us = leaf(ui, "scroll", cmd_ui_scroll, UI, "scroll a container, optionally until a target shows")
    us.add_argument("selector", help="the scroll container, e.g. scrollable=true")
    us.add_argument("--dir", default="down", choices=["up", "down", "left", "right"])
    us.add_argument("--until", help="selector to scroll until visible")
    usw = leaf(ui, "swipe", cmd_ui_swipe, UI, "swipe from an element")
    usw.add_argument("selector")
    usw.add_argument("--dir", required=True, choices=["up", "down", "left", "right"])
    for sp in (ut, uty, us, usw):
        sp.add_argument("--first", action="store_true", help="use the first match instead of failing on several")
    for sp in (ut, uty):
        sp.add_argument("--wait", type=positive_int, metavar="MS", help="wait up to MS for the element")
    uxy = leaf(ui, "tapxy", cmd_ui_tapxy, UI, "tap coordinates (last resort; prefer selectors)")
    uxy.add_argument("x", type=int)
    uxy.add_argument("y", type=int)
    uh = leaf(ui, "hold", cmd_ui_hold, UI, "long-press coordinates (take them from `ui find`)")
    uh.add_argument("x", type=int)
    uh.add_argument("y", type=int)
    uh.add_argument("--ms", type=positive_int, default=800)
    uk = leaf(ui, "key", cmd_ui_key, UI, "press back, home, enter, volume-up, ... or a keycode")
    uk.add_argument("key")
    ush = leaf(ui, "shot", cmd_ui_shot, UI, "screenshot to PATH")
    ush.add_argument("path")
    ush.add_argument("--annotate", action="store_true", help="number the elements")
    rc = leaf(ui, "record", cmd_ui_record, EMULATOR, "record the screen to captures/verify/$TSCTL_RUN/NAME.mp4 (start, then stop)",
              "example: tsctl ui record start beacons/flow ; ...drive the app... ; tsctl ui record stop")
    rc.add_argument("action", choices=["start", "stop"])
    rc.add_argument("name", nargs="?", default="recording")
    sn = leaf(ui, "snap", cmd_ui_snap, UI, "evidence: screenshot + layout to captures/verify/$TSCTL_RUN/NAME.{png,layout.json}")
    sn.add_argument("name", help="e.g. beacons/01-list")

    lg = leaf(top, "logs", cmd_logs, ACT, "app logcat (by pid), the crash buffer, or clear")
    lg.add_argument("--crash", action="store_true", help="crash buffer only; empty means no crashes")
    lg.add_argument("--all", action="store_true", help="all processes")
    lg.add_argument("--clear", action="store_true")
    lg.add_argument("--lines", type=positive_int, default=300)
    lg.add_argument("--out", help="write to a file instead of stdout")
    lg.add_argument("--grep", help="keep only lines matching this regex")

    dp = leaf(env, "display", cmd_display, EMULATOR,
              "screen size, density, font scale, dark mode, or rotation; undo with `env display reset`",
              "examples: tsctl env display size 720x1280 ; tsctl env display font-scale 1.3 ; tsctl env display dark on ; tsctl env display reset")
    dp.add_argument("setting", choices=[*DISPLAY_SETTINGS, "status", "reset"])
    dp.add_argument("value", nargs="?")

    leaf(top, "notifications", cmd_notifications, ACT, "active notifications posted by the app")

    a = leaf(top, "adb", cmd_adb, DESTRUCTIVE, "escape hatch: run adb on the resolved target, gated like a destructive action",
             "example: tsctl adb -- shell dumpsys battery set level 23")
    a.add_argument("rest", nargs=argparse.REMAINDER)
    return root


# ---------------------------------------------------------------- main

def command_name(args):
    parts = [args.command]
    sub = getattr(args, f"{args.command}_cmd", None)
    return " ".join(parts + ([sub] if sub else []))


def touched_packages(ctx, policy):
    touched = [ctx.package]
    if ctx.args.fn is cmd_test_android:
        touched.append(BASE_PACKAGE + ".test")
    if ctx.args.fn is cmd_adb:
        touched.append(" ".join(ctx.args.rest))
    if policy.focus_check and ctx.target and not ctx.target.emulator:
        # Screen input lands on whatever app is in front.
        touched.append(parse_focused_package(text(ctx.target.shell("dumpsys", "window", check=False))))
    return touched


def render(ctx, out):
    if ctx.json:
        payload = {"ok": out.code == 0, "data": out.data}
        if ctx.dry_run:
            payload["dry_run"] = ctx.planned
        print(json.dumps(payload, default=str))
        return
    for step in ctx.planned:
        is_command = step.startswith(("adb ", "android ", "./gradlew", "ANDROID_SERIAL="))
        print(f"DRY-RUN would {'run: ' if is_command else ''}{step}")
    if out.text:
        print(out.text)


def main(argv=None):
    sys.stdout.reconfigure(line_buffering=True)
    signal.signal(signal.SIGPIPE, signal.SIG_DFL)
    args = build_parser().parse_args(argv)
    ctx = Ctx(args=args, device=getattr(args, "device", None), variant=getattr(args, "variant", "debug"),
              dry_run=getattr(args, "dry_run", False), approved=getattr(args, "user_approved", None),
              json=getattr(args, "json", False))
    name = command_name(args)
    policy = args.policy
    try:
        old = prune_captures(ctx.dry_run)
    except OSError as e:
        old = []
        log(f"warning: could not prune old captures: {e}")
    if old:
        verb = "would delete" if ctx.dry_run else "deleted"
        log(f"{verb} {len(old)} captures and backups older than {RETENTION_DAYS} days: " + ", ".join(str(p.relative_to(CAPTURES)) for p in old))
    try:
        check_dry_run(policy, ctx.dry_run, name)
        if policy.device:
            ctx.target = resolve_target(ctx.device)
            decide(policy, is_emulator=ctx.target.emulator, serial=ctx.target.serial,
                   touched=touched_packages(ctx, policy), dry_run=ctx.dry_run, approved=ctx.approved,
                   granted=ctx.package in load_grants().get(ctx.target.serial, []),
                   # A grant covers the package, not raw adb, which can touch anything.
                   grantable=args.fn is not cmd_adb,
                   action=f"`tsctl {name}` ({ctx.package})")
        out = args.fn(ctx)
        render(ctx, out)
        return out.code
    except (Refused, Failed, UsageError) as e:
        code = {Refused: 3, Failed: 1, UsageError: 2}[type(e)]
        if ctx.json:
            print(json.dumps({"ok": False, "error": str(e), "refused": code == 3}))
        log(("" if code == 3 else "error: ") + str(e))
        return code
    except KeyboardInterrupt:
        return 130
