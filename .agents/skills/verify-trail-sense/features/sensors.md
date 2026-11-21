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
- **Change.** Change a sensor value. Run `$T sensor set pressure 990.5`. The barometer card shows ~990.5 hPa.
- **Proof.** Snap before and after `sensor set`.

## Gotchas

- Light and step counter cards report unavailable on the emulator.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
