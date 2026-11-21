---
name: verify-trail-sense
description: Drive the real Trail Sense Android app on an emulator to prove a change works from the user's point of view, using the tsctl control script (build/install variants, open a tool by intent, UI taps via the android CLI, GPS/sensor injection, permissions, shared prefs, Room database, lint, unit and instrumented tests). Use when asked to verify, demo, screenshot, or reproduce behavior in the running app, or to set up app state on an emulator. Prefer tsctl over scripts/*.sh.
---

# Verify Trail Sense

Trail Sense is a single-activity Android app (`com.kylecorry.trail_sense.main.MainActivity`) with ~45 tools. Users reach tools from the **Tools** tab (bottom nav, search box at the top), the other bottom-nav tabs, quick actions, and widgets. Verification means driving that UI on an emulator and capturing evidence.

All work goes through one script:

```bash
T=.agents/skills/verify-trail-sense/scripts/tsctl   # from the repo root (.claude/skills links here)
$T --help                 # command groups, examples, exit codes
$T <group> --help         # subcommands, e.g. $T ui --help
$T <group> <cmd> --help   # flags for one command
```

**Global flags** (before or after the command):

- `--device SERIAL`
- `--variant debug|staging|nightly|nightlyRelease|release|playStore|github` (default `debug`)
- `--dry-run`
- `--user-approved SERIAL`
- `--json`: prints one `{"ok": ..., "data": ..., "dry_run": [...]}` object on stdout. Gradle and progress output go to stderr.

**Exit codes:** 0 ok, 1 failed, 2 usage error, 3 refused by a safety gate.

## Safety rules (read before touching a device)

Every command declares a risk level, and tsctl enforces it in one place before the command runs.

- **Emulators need no approval**, even for data-destructive commands.
- **Default target is the single running emulator.** tsctl never falls back to a physical device. A phone is only used with an explicit `--device <serial>`.
- **On a physical device, every command except `devices` and `doctor` needs the user's approval**: launching, tapping, screenshots, prefs, db, logs, installs. There are two ways to give it:
  - **One action:** after the user approves that specific action in chat, rerun with `--user-approved <serial>`. It covers that one command only.
  - **Full control of a package:** when the user says you have full control of a package on that device, run `$T device grant <serial> <package>` once. Commands for that package then run without asking again. `$T device grants` lists the grants and `$T device revoke <serial> <package>` removes one. A grant doesn't cover the raw `adb --` escape hatch.

  Never pass the flag or create a grant on your own judgment.
- **Dry runs on a phone:** `--dry-run` of `app install`, `app uninstall`, `app clear`, `test android`, and `adb --` only prints the steps, so it needs no approval. Run it first and show the user the output before asking. Dry runs of `pref`, `db`, and `app seed` read app-private files, so they need approval too. A command with no dry-run mode rejects `--dry-run` with exit 2 rather than acting for real.
- **`com.kylecorry.trail_sense.nightly_release` is never touched on a physical device**, even with approval or a grant. tsctl refuses the `nightlyRelease` variant, `adb --` arguments that mention it, and UI input while it is in front. It can't be granted. Don't try to work around this.
- **The debug variant has the same package as the user's F-Droid, Play, and GitHub install.** If an install fails with a signature mismatch, report it. Never uninstall to "fix" it.
- **Emulator settings:** tsctl starts emulators with `--gpu swiftshader_indirect -cores 2 -memory 2048 -no-snapshot-save` and won't start a second one. With swiftshader, host memory grows ~125 MB per UI session. When `doctor` or `emu status` reports memory above 4500 MB, restart the emulator (`emu stop`, `emu start`). Ask the user before a long unattended instrumented run.
- **Don't use `--gpu host` by default:** on the user's machine (Iris Xe through Mesa, emulator 37.2.12), it crashed twice on 2026-11-21 with a segfault in `libgfxstream_backend.so`, right after app relaunches. If the emulator ever vanishes from `adb devices`, check `coredumpctl list qemu-system-x86_64`.
- **Isolation:** there is one emulator and one app data directory, so don't run two verification sessions at once. tsctl only stops an emulator that tsctl started. If the user's own emulator is running, it is reused and left running.

## Launch

```bash
$T emu start            # reuses a running emulator; otherwise boots AVD Android_16 (API 36) headless, ~40 s
$T app install          # assembleDebug, then `android install` (--no-build to skip Gradle)
$T app seed --clear     # empty data dir -> usable app (see below)
$T doctor               # must end with "doctor: OK"
```

`app seed` does the following:

1. Clears the data (with `--clear`) and launches once so the app creates its prefs file.
2. Force-stops the app.
3. Sets onboarding done, the disclaimer as shown, test units (feet/miles, lbs, 12 h), the GPS altimeter mode, and no GPS accuracy filter. These match `TestUtils.setupDefaultPreferences`, except for its test-only compact mode and bottom navigation.
4. Grants location, background location, camera, notifications, activity recognition, and the exact-alarm appop.
5. Relaunches the app.

Seeding is idempotent: rerunning it converges to the same state.

The app is **ready** when `doctor` prints `onboarding completed: True` and `doctor: OK`. Without seeding, MainActivity redirects to onboarding and `open` does nothing.

Other variants:

- `staging`: `$T --variant staging app install`. Release code signed with the debug key, package `.staging`. It has no run-as, so the prefs and db commands don't work.
- `nightly` and `github`: need signing properties.
- `release` and `playStore`: build only (`$T build release`).

## Doctor

`$T doctor` (or `$T doctor --json`) is read-only. It reports:

- the target, and whether it counts as an emulator and why
- the Android version and boot state
- the AVD, whether tsctl started it, and its host memory
- the installed version and update time, next to the local APK's build time
- whether the app is debuggable, and whether onboarding is complete (emulator only)
- the focused window
- a `problem:` line with the fix for each issue

Exit 1 means not ready. A stale install (APK newer than `lastUpdateTime`) means run `$T app install`.

## Drive

**Open a tool directly** (fast precondition, not the user path):

```bash
$T tools [filter]        # names and ids parsed from Tools.kt, e.g. "7 beacons"
$T open beacons          # am start -a com.kylecorry.trail_sense.OPEN_TOOL --el tool_id 7
```

**User path to a tool:** tap the Tools tab, type in the search box, then tap the tile:

```bash
$T ui tap id=action_experimental_tools
$T ui type Beacons id=search_view_edit_text
$T ui tap 'text=/^Beacons$/,id=title'
```

**UI commands** wrap `android layout` and `android input`.

- Selectors are comma-separated `key=value` pairs: `text=`, `id=`, `desc=`, `class=`, `clickable=true`, and so on.
- Plain values are case-insensitive substrings. Use `/regex/` for an exact match.
- An ambiguous selector fails, lists the matches, and suggests a fix. Tighten the selector, or pass `--first`.

```bash
$T ui find "Verify"                 # one JSON line per element (text/desc/id contains); exit 1 if none
$T ui wait "Verify Camp" --timeout 10
$T ui tap id=create_btn
$T ui tap 'text=/^Beacon$/'         # "Beacon" alone would also match "Beacons"
$T ui type "Verify Camp" 'id=/:id\/text_input$/'
$T ui scroll scrollable=true --until 'text=Icon'
$T ui key back
$T ui hold 360 600                  # long press; coordinates from `ui find`
$T ui tapxy 540 900                 # last resort; prefer selectors
$T ui layout [--full] [--out f.json]
```

Common handles:

- Toolbar title: `id=toolbar_title`.
- Toolbar action: `id=toolbar_right_button` (save checkmark, menus).
- Floating create button: `id=create_btn`.
- Bottom nav: `desc=Navigation|Map|Weather|Astronomy|Tools`, or ids `action_navigation`, `mapFragment`, `action_weather`, `action_astronomy`, `action_experimental_tools`.
- Dialog buttons: `text=/^OK$/`, `text=/^Cancel$/`.

**Environment** (emulator only):

```bash
$T geo 42.3601 -71.0589 20          # GPS fix: lat lon [alt m above the ellipsoid]
$T sensor set pressure 990.5        # hPa; also magnetic-field X Y Z, acceleration X Y Z, temperature, humidity, ...
$T sensor get pressure ; $T sensor status
$T perm grant location camera ; $T perm revoke notifications ; $T perm list   # aliases + "all"; revoke kills the app
```

The emulator has no step counter and no light sensor, so Pedometer and Light Meter aren't available. It has no real camera image (only an emulated scene) and no torch.

**Preferences** (debug and nightly only). KEY is a raw key, or `@string_resource_name` resolved through `strings.xml`:

```bash
$T pref keys units                  # find keys: "@pref_distance_units  distance_units"
$T pref get @pref_use_24_hour       # exit 1 when unset
$T pref list [filter]
$T pref set @pref_use_24_hour true  # type defaults to the existing one; --type bool|int|long|float|string; values are validated
$T pref delete some_key
```

`set` and `delete` force-stop the app first, because the app caches prefs in memory and would overwrite the edit. Writes go to a temp file that is renamed into place, so an interrupted write leaves the old file intact. The change shows on the next `app launch` or `open`.

**Database** (Room `trail_sense`, debug and nightly only):

```bash
$T db tables ; $T db schema beacons
$T db query "select _id, name, latitude, longitude from beacons" [--json]   # reads a pulled copy; the app keeps running
$T --dry-run db exec "delete from notes where _id=3"                        # applies to a scratch copy and reports rows changed
$T db exec "insert into notes(title, contents, created) values ('x','y',0)" # back up, force-stop, push atomically
$T db pull /some/dir
```

**Backups:** before changing or wiping app data, tsctl copies the app's shared prefs and database to `captures/verify/backups/<time>-<reason>/` and prints `backup: <dir>`. It does this for `app clear`, `app uninstall`, `app seed`, `pref set`, `pref delete`, `db exec`, `test android` (for the debug package), and `app restore` itself.

- Undo with `$T app restore <dir>`. It backs up the current data first, so a restore can be undone too.
- After `app uninstall`, run `$T app install` before restoring.
- Only prefs and the database are covered. Other app files (imported maps, photos) are not.
- Non-debuggable variants (staging) can't be backed up; tsctl warns and continues.
- Dry runs only list the backup step. Old backups are deleted automatically (see Evidence).

**App lifecycle and logs:** `$T app launch|stop|kill|idle|services|current|clear|uninstall|restore`, `$T logs [--lines N] [--all] [--out f]`, `$T logs --crash`, `$T logs --clear`.

**Background and widget state:**

- `$T app kill` drops the process but keeps widgets and their PendingIntents. Use it, not `app stop`: force-stop cancels widget PendingIntents, so a widget tap then does nothing.
- `$T app idle` kills the app and puts it in the restricted standby bucket with an idle uid. `$T app idle --off` undoes it.
- `$T doze on|off` forces deep Doze (emulator only; waits 3 s after `on`). Background `startService` limits only show with `doze on` plus `app idle`.
- `$T app services` lists the app's running services.
- `$T widget add <name>` adds a widget through the Pixel launcher picker (emulator only) and saves its config screen if one shows. `$T widget tap` taps the widget on the home screen.

**Build and test:**

- `$T build [variant...]`
- `$T lint`: detekt, or Android lint with `--android`.
- `$T test unit 'com.kylecorry.trail_sense.tools.beacons*'`
- `$T test android com.kylecorry.trail_sense.tools.beacons.ToolBeaconsTest [--timeout 600] [--shard 0/4]`. It pins `ANDROID_SERIAL` to the target, and reports go to `app/build/reports/androidTests/connected`.

**Escape hatch:** `$T adb -- shell <anything>` runs on the resolved target, gated like a destructive action.

## Evidence

```bash
export TSCTL_RUN=beacons-$(date +%Y%m%d-%H%M%S)
$T ui snap beacons/01-list     # -> captures/verify/$TSCTL_RUN/beacons/01-list.png + .layout.json
$T db query "..." > captures/verify/$TSCTL_RUN/beacons/02-db.txt
$T logs --crash --out captures/verify/$TSCTL_RUN/crash.txt
```

`captures/` is gitignored and survives cleanup. Each tsctl run deletes anything in `captures/verify/` untouched for more than 7 days: whole run folders, and individual backups in `backups/` (and the legacy `db-backups/`). A dry run only lists what it would delete. Copy evidence elsewhere if it must outlive a week.

Proof standards:

- **Exercise the real user path:** taps on the screens a user sees. `open`, `pref`, `db exec`, and `geo` set up preconditions. They never stand in for the action under test.
- **Snap before and after the action**, not only the final screen.
- **Verify side effects with a second view:** `db query` for rows, `pref get` for settings, and the list screen after a save. Reopening the screen proves persistence.
- **Check for crashes:** `$T logs --crash` must be empty at the end. A crash invalidates the run.
- **Dry runs change nothing:** install keeps `lastUpdateTime`, and `db exec` leaves rows unchanged. A dry run still reads the device (dumpsys, run-as cat), and for `db exec` it pulls the database to a temp dir.
- **Report what can't be verified:** when the emulator can't meet a precondition (missing sensor, camera, cell radio), report the journey as not verified with the reason. Don't swap in a weaker path.

## Cleanup

```bash
$T app stop             # stop the app
$T emu stop             # only stops an emulator tsctl started (it refuses others)
```

Never kill emulators or Gradle by process name. If you changed prefs or the db on the user's own emulator, put them back with `$T app restore <backup>` or tell the user. Evidence under `captures/verify/` stays.

## Helpers

- `scripts/tsctl` is the entry point.
- `scripts/core.py` holds the pure logic: the safety policy, variants, and parsers.
- `scripts/cli.py` holds the commands. Each leaf command declares a `Policy` (risk, app data, dry-run, emulator-only), and `main()` gates on it before running the handler.
- **Tests:** after changing tsctl, run its unit tests. They need no device and include a structural check that every command declares a policy:

  ```bash
  python3 -B -m unittest discover -s .agents/skills/verify-trail-sense/scripts
  ```

- **Gates on a real phone:** to check them without one, set `TSCTL_FORCE_PHYSICAL=1`. tsctl then treats the emulator as physical (pass `--device emulator-5554`).
- **State:** `~/.cache/tsctl/` holds `emulators.json`, which records the emulators tsctl started and is pruned of dead ones on every read, and `grants.json`, which holds the device grants.

## Feature map

`features/README.md` indexes one file per user guide (`guides/en-US/guide_tool_*.txt`). Each feature file links its guide, whose section headings are the sub-features, instead of copying them. It adds the tool id, the preconditions, the handles, and what proves the feature works. Read the matching file before driving a tool. Update it when you learn something new, especially the Gotchas and the "proven" column.
