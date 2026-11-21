# Settings

Use Settings to customize Trail Sense, calibrate your sensors, and back up your data. Each section below describes a group of settings and the path to open it. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_settings.txt`](../../../../guides/en-US/guide_tool_settings.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Settings` → tap the tile.
- Quick action (see the guide's Quick action section).
- Precondition shortcut only: `$T open settings`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **Units.** Open Settings, then Units, and change distance units. Run `$T open settings`, `$T ui tap text=Units`. The preference summary changes.
- **Side effect.** Stored value. Run `$T pref get @pref_distance_units`. Shows the new value (seed sets feet_miles).
- **Proof.** Snap the settings screen and save `pref get` output; reopen a tool that shows distances to confirm the effect.

## Gotchas

- Settings writes through the app's preference cache; read with `pref get` after the change, don't write with `pref set` while the screen is open.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
