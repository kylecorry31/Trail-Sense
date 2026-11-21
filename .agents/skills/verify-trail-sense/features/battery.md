# Battery

Use the Battery tool to monitor your battery consumption, find out which services drain it, and reduce Trail Sense's power use. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_battery.txt`](../../../../guides/en-US/guide_tool_battery.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Battery` → tap the tile.
- Quick action (see the guide's Quick action section).
- Precondition shortcut only: `$T open battery`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- Optionally fake the battery: `$T adb -- shell dumpsys battery set level 23` (reset with `dumpsys battery reset`).

- **Status.** Open the tool. Run `$T open battery`. `id=battery_percentage` reads `100%` (or the faked level); `battery_capacity`, `battery_current`, `battery_deep_sleep` and `running_services` follow.
- **Power saving.** Turn on power saving mode per the guide. Run `$T ui tap id=low_power_mode_switch` (labelled `Power saving mode`). `$T pref get pref_low_power_mode` flips `true`/`false` (driven both ways on 2026-11-21); tap it again to restore.
- **Proof.** Snap the status with the faked level visible; `pref get` for the power saving key after toggling in the UI.

## Gotchas

- Always `dumpsys battery reset` afterwards, or the emulator keeps the fake level.
- History needs recorded samples over time; a fresh install has none.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
