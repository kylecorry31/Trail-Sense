# Sensors

Use the Sensors tool to test the sensors on your device. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_sensors.txt`](../../../../guides/en-US/guide_tool_sensors.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Sensors` → tap the tile.
- Quick action (see the guide's Quick action section).
- Home screen or in-app widgets (see the guide's Widgets section).
- Precondition shortcut only: `$T open sensors`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **List.** Open the tool. Run `$T open sensors`. Sensor cards with live values.
- **Change.** Change a sensor value. Run `$T env sensor set pressure 990.5`. The barometer card shows `29.25 in` (pressure units default to inHg; `990.5 hPa` only if the unit pref is hPa).
- **Proof.** Snap before and after `sensor set`.

## Gotchas

- There are no Light or Step counter cards at all on the emulator.
- The screen refreshes every 500 ms, so `android layout` never goes idle. tsctl retries `ui layout`, `ui find`, `ui snap` with `--no-idle`; `ui tap` selectors on this screen were not driven.
