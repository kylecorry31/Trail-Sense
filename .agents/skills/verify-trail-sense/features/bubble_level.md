# Bubble Level

Use the Bubble Level tool to check whether a surface is flat, for example when you set up a tent or a cooking stove. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_bubble_level.txt`](../../../../guides/en-US/guide_tool_bubble_level.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Bubble Level` → tap the tile.
- Precondition shortcut only: `$T open bubble-level`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- Flat device: `$T sensor set acceleration 0 0 9.81`.

- **Level.** Open the tool with the device flat. Set `$T sensor set acceleration 0 0 9.81`, then `$T open bubble-level`. `id=toolbar_title` reads `X: 0.0°\nY: 0.0°\nAngle: 0.0°`.
- **Tilt.** Tilt the device. Run `$T sensor set acceleration 1.7 0 9.66` before opening the tool: the title reads `X: 10.0°` (`Angle: 10.0°`). Changing it while the tool is open settles slowly (1.7° two seconds later), so reopen the tool or wait. Driven on 2026-11-21.
- **Proof.** Snap flat and tilted; the angle text changes accordingly.

## Gotchas

- `sensor set acceleration` values are m/s² as X Y Z; restore `0 9.81 0` (upright) or `0 0 9.81` (flat) after.
