# Water Boil Timer

Use the Water Boil Timer tool to find out how long to boil water to make it safe to drink. Boiling time varies with elevation, so the tool uses your current elevation. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_water_boil_timer.txt`](../../../../guides/en-US/guide_tool_water_boil_timer.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Water Boil Timer` → tap the tile.
- Precondition shortcut only: `$T open water-boil-timer`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- `$T geo 42.3601 -71.0589 20` (elevation sets the time).

- **Timer.** Start the timer. Run `$T open water-boil-timer`, `$T ui find` the start button. Countdown runs; a notification appears.
- **Proof.** Snap the running timer; change the altitude with `$T geo 39.7392 -104.9903 1600` and confirm the time is longer (the tool switches to the longer time at 1000 m and above, per WaterService).

## Gotchas

- Elevation comes from the altimeter mode; seed sets GPS mode.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
