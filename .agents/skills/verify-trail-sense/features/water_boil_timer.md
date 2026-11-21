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

- **Timer.** Start the timer. Run `$T open water-boil-timer`: `id=time_left` reads `60` (seconds) and `id=boil_button` reads `Start`. After `$T ui tap id=boil_button` the button reads `Cancel`, `time_left` counts down (54 after a few seconds) and the chips (`chip_auto`, `chip_1_min`, `chip_3_min`) are hidden. The Water Boil Timer notification channels exist in `dumpsys notification`; the live notification text was not matched.
- **Proof.** Snap the running timer; change the altitude with `$T geo 39.7392 -104.9903 1600` (leave and reopen the tool so the altimeter re-reads) and confirm the time is longer (the tool switches to the longer time at 1000 m and above, per WaterService).

## Gotchas

- Elevation comes from the altimeter mode; seed sets GPS mode.
- The 1000 m / 180 s branch was not driven.
