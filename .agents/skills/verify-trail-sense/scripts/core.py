"""Pure logic for tsctl: safety policy, variants, and parsers. No device or filesystem access."""

import argparse
import json
import re
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from typing import NamedTuple, Optional

BASE_PACKAGE = "com.kylecorry.trail_sense"
PROTECTED_PACKAGE = BASE_PACKAGE + ".nightly_release"


class Variant(NamedTuple):
    package: str
    debuggable: bool
    installable: bool
    note: str


VARIANTS = {
    "debug": Variant(BASE_PACKAGE, True, True, "Same package as the F-Droid/Play/GitHub release builds."),
    "staging": Variant(BASE_PACKAGE + ".staging", False, True, "Release build signed with the debug key; no run-as."),
    "nightly": Variant(BASE_PACKAGE + ".nightly", True, True, "Needs -Dnightly_store_file signing properties."),
    "nightlyRelease": Variant(PROTECTED_PACKAGE, False, True, "Never touched on a physical device."),
    "release": Variant(BASE_PACKAGE, False, False, "Unsigned F-Droid build; build only."),
    "playStore": Variant(BASE_PACKAGE, False, False, "Unsigned Play build; build only."),
    "github": Variant(BASE_PACKAGE, False, False, "Needs GitHub signing properties."),
}


class Refused(Exception):
    """A safety gate said no. Exit code 3."""


class Failed(Exception):
    """The command could not do its job. Exit code 1."""


class UsageError(Exception):
    """The command was called wrongly. Exit code 2."""


# ---------------------------------------------------------------- safety policy

RISKS = ("inspect", "act", "destructive")


@dataclass(frozen=True)
class Policy:
    """What a command does to a device. Every leaf command declares one; main() enforces it.

    risk: inspect (read-only, no app data), act (drives or reads the device), destructive
    (can lose user data). app_data: reads app-private files through run-as.
    """
    device: bool = True
    risk: str = "act"
    app_data: bool = False
    dry_run: bool = False
    emulator_only: bool = False
    focus_check: bool = False

    def __post_init__(self):
        if self.risk not in RISKS:
            raise ValueError(f"unknown risk {self.risk}")


NO_DEVICE = Policy(device=False, risk="inspect")
NO_DEVICE_DRY = Policy(device=False, risk="inspect", dry_run=True)


def check_dry_run(policy: Policy, dry_run: bool, command: str):
    if dry_run and not policy.dry_run and policy.risk != "inspect":
        raise UsageError(f"`{command}` has no --dry-run mode; it would act for real. Drop --dry-run.")


def validate_grant(package: str):
    if PROTECTED_PACKAGE in package:
        raise Refused(f"Refused: {PROTECTED_PACKAGE} can never be granted.")
    if not re.fullmatch(r"[A-Za-z][\w]*(\.[A-Za-z][\w]*)+", package):
        raise UsageError(f"{package!r} is not a package name.")


def decide(policy: Policy, *, is_emulator: bool, serial: str, touched: list, dry_run: bool,
           approved: Optional[str], action: str, granted: bool = False, grantable: bool = True):
    """Raises Refused/Failed when the command may not run on this target.

    On a physical device a command runs when it only inspects, when the user approved this
    action (approved == serial), or when the user gave full control of the package (granted).
    """
    if not policy.device:
        return
    if policy.emulator_only and not is_emulator:
        raise Failed(f"{action} only works on an emulator; {serial} is a physical device.")
    if is_emulator:
        return
    if any(PROTECTED_PACKAGE in t for t in touched if t):
        raise Refused(f"Refused: {PROTECTED_PACKAGE} is never touched on a physical device ({serial}).")
    if policy.risk == "inspect":
        return
    if policy.risk == "destructive" and dry_run and not policy.app_data:
        return
    if approved == serial or (granted and grantable):
        return
    level = "a data-destructive action" if policy.risk == "destructive" else "an action"
    hint = ""
    if policy.risk == "destructive" and policy.dry_run and not policy.app_data:
        hint = " Use --dry-run first to show them what it would do."
    raise Refused(
        f"Refused: {action} on PHYSICAL device {serial} is {level} that needs user approval.\n"
        f"Ask the user to approve exactly this action, then rerun with --user-approved {serial}.{hint}"
        + (f"\nIf the user gives you full control of this package on this device, run `tsctl grants add {serial} <package>` instead."
           if grantable else "")
    )


def classify(serial: str, qemu_props: list, forced_physical: bool):
    """Returns (is_emulator, reason). Anything unconfirmed counts as physical."""
    if forced_physical:
        return False, "TSCTL_FORCE_PHYSICAL=1"
    if not serial.startswith("emulator-"):
        return False, "serial is not emulator-*"
    if "1" not in qemu_props:
        return False, "no qemu property"
    return True, "emulator-* serial and qemu property"


# ---------------------------------------------------------------- argument types

def shard(text: str):
    m = re.fullmatch(r"(\d+)/(\d+)", text)
    if not m or int(m.group(1)) >= int(m.group(2)):
        raise argparse.ArgumentTypeError("use INDEX/TOTAL with 0 <= INDEX < TOTAL, e.g. 0/4")
    return int(m.group(1)), int(m.group(2))


def positive_int(text: str):
    if not text.isdigit() or int(text) <= 0:
        raise argparse.ArgumentTypeError("must be a positive whole number")
    return int(text)


# ---------------------------------------------------------------- tools and string resources

def parse_tool_ids(tools_kt: str) -> dict:
    return {
        name.lower().replace("_", "-"): int(value)
        for name, value in re.findall(r"const val ([A-Z_]+) = (\d+)L", tools_kt)
        if not name.startswith("QUICK_ACTION")
    }


def resolve_tool(name: str, ids: dict) -> int:
    if name.isdigit():
        return int(name)
    key = name.lower().replace("_", "-").replace(" ", "-")
    if key in ids:
        return ids[key]
    matches = [k for k in ids if key in k]
    if len(matches) == 1:
        return ids[matches[0]]
    raise UsageError(f"Unknown tool '{name}'. " + (f"Ambiguous: {matches}" if matches else "Run `tsctl tools`."))


def parse_string_resources(strings_xml: str) -> dict:
    return {e.get("name"): (e.text or "") for e in ET.fromstring(strings_xml).iter("string")}


def resolve_pref_key(key: str, strings: dict) -> str:
    if not key.startswith("@"):
        return key
    if key[1:] not in strings:
        raise UsageError(f"No string resource named {key[1:]} in strings.xml. Try `tsctl pref keys {key[1:]}`.")
    return strings[key[1:]]


# ---------------------------------------------------------------- shared preferences XML

PREF_TAGS = {"bool": "boolean", "int": "int", "long": "long", "float": "float", "string": "string"}


def parse_prefs(data: bytes):
    if not data.strip().startswith(b"<?xml"):
        return None
    return ET.fromstring(data)


def pref_value(elem):
    if elem.tag == "string":
        return elem.text or ""
    if elem.tag == "set":
        return [c.text or "" for c in elem]
    return elem.get("value")


def prefs_dict(root) -> dict:
    return {e.get("name"): {"value": pref_value(e), "type": e.tag} for e in root}


def _validate(kind: str, key: str, value: str) -> str:
    try:
        if kind == "bool":
            if value.lower() not in ("true", "false"):
                raise ValueError
            return value.lower()
        if kind in ("int", "long"):
            return str(int(value))
        if kind == "float":
            return str(float(value))
    except ValueError:
        raise UsageError(f"{key} is a {kind} pref; {value!r} is not a valid {kind}.")
    return value


def apply_pref_edits(root, edits) -> list:
    """edits: (key, kind or None, value or None to delete). Mutates root; returns change records."""
    changes = []
    for key, kind, value in edits:
        old = next((e for e in root if e.get("name") == key), None)
        before = pref_value(old) if old is not None else None
        if old is not None:
            root.remove(old)
        if value is None:
            changes.append({"key": key, "before": before, "after": None})
            continue
        if kind is None:
            kind = {"boolean": "bool"}.get(old.tag, old.tag) if old is not None else "string"
        if kind not in PREF_TAGS:
            raise UsageError(f"{key} is a {kind} pref; tsctl can only set {', '.join(PREF_TAGS)}.")
        value = _validate(kind, key, value)
        elem = ET.SubElement(root, PREF_TAGS[kind], {"name": key})
        if kind == "string":
            elem.text = value
        else:
            elem.set("value", value)
        changes.append({"key": key, "before": before, "after": value, "type": kind})
    return changes


def serialize_prefs(root) -> bytes:
    ET.indent(root, space="    ")
    return b"<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n" + ET.tostring(root, encoding="utf-8")


# ---------------------------------------------------------------- device output parsers

def parse_installed_info(dumpsys: str, package: str):
    if f"Package [{package}]" not in dumpsys:
        return None
    info = {"package": package}
    for key in ("versionName", "versionCode", "lastUpdateTime", "firstInstallTime"):
        m = re.search(rf"{key}=(\S+(?: \d\d:\d\d:\d\d)?)", dumpsys)
        if m:
            info[key] = m.group(1)
    m = re.search(r"pkgFlags=\[([^\]]*)\]", dumpsys)
    info["debuggable"] = bool(m and "DEBUGGABLE" in m.group(1))
    return info


def parse_granted_permissions(dumpsys: str) -> dict:
    return {p: g == "true" for p, g in re.findall(r"(android\.permission\.[A-Z_]+): granted=(true|false)", dumpsys)}


def parse_flash_units(dumpsys_camera: str) -> dict:
    """Camera id -> whether its static info reports android.flash.info.available."""
    flash = {}
    camera = None
    lines = dumpsys_camera.splitlines()
    for i, line in enumerate(lines):
        header = re.match(r"== Camera HAL device \S+/(\S+) .*static information", line)
        if header:
            camera = header.group(1)
        elif camera is not None and camera not in flash and "android.flash.info.available" in line and i + 1 < len(lines):
            flash[camera] = "TRUE" in lines[i + 1]
    return dict(sorted(flash.items()))


def parse_torch_state(dumpsys_systemui: str):
    """SystemUI's FlashlightController state: the torch camera, whether it is on, and whether it is usable."""
    m = re.search(r"FlashlightController state:\s*mCameraId=(\S*)\s*mFlashlightEnabled=(\w+)\s*mTorchAvailable=(\w+)",
                  dumpsys_systemui)
    if not m:
        return None
    return {"camera": m.group(1) if m.group(1) != "null" else None, "on": m.group(2) == "true", "available": m.group(3) == "true"}


def parse_focused_package(dumpsys_window: str) -> str:
    m = re.search(r"mCurrentFocus=Window\{\S+ \S+ ([^/}]+)", dumpsys_window)
    return m.group(1) if m else ""


def flatten_layout(data) -> list:
    """Element nodes from every non-system window of `android layout` output."""
    nodes = []

    def walk(item, window):
        if isinstance(item, list):
            for child in item:
                walk(child, window)
        elif isinstance(item, dict):
            if "window-title" in item:
                if item.get("type") == "SYSTEM":
                    return
                window = item.get("window-title")
            elif "class" in item:
                nodes.append({k: v for k, v in item.items() if k not in ("content", "children")} | {"window": window})
            for key in ("content", "children"):
                walk(item.get(key), window)

    walk(data, None)
    return nodes


def node_matches(node: dict, needle: str) -> bool:
    hay = " ".join(node.get(k, "") for k in ("text", "content-desc", "resource-id"))
    return needle.lower() in hay.lower()


def describe_node(n: dict) -> str:
    rid = n.get("resource-id", "").split(":id/")[-1]
    parts = [f"id={rid}" if rid else "", f"text={n['text']!r}" if n.get("text") else "",
             f"desc={n['content-desc']!r}" if n.get("content-desc") else "", f"at {n.get('center', '?')}"]
    return " ".join(p for p in parts if p)


def parse_input_results(output: str):
    """Results of an `android input` command, or None when it printed something else."""
    try:
        return json.loads(output).get("results", [])
    except (ValueError, AttributeError):
        return None


# ---------------------------------------------------------------- capture retention

def expired(entries: dict, now: float, max_age_seconds: float) -> list:
    """entries: path -> newest modification time inside it. Returns paths untouched for longer than max age."""
    return sorted(p for p, newest in entries.items() if now - newest > max_age_seconds)


# ---------------------------------------------------------------- emulator ownership

def prune_state(state: dict, alive_pids: set, connected: set) -> dict:
    """Keeps only emulators tsctl started that are still running and attached."""
    return {s: e for s, e in state.items() if e.get("pid") in alive_pids and s in connected}
