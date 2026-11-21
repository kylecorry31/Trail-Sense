# Clinometer

Use the Clinometer tool to measure angles. You can use it to find the slope of a hill, the height of an object, or the distance to an object. You can also use it to check the avalanche risk of a slope. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_clinometer.txt`](../../../../guides/en-US/guide_tool_clinometer.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Clinometer` → tap the tile.
- Precondition shortcut only: `$T open clinometer`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- Upright device: `$T sensor set acceleration 0 9.81 0`.

- **Slope.** Open the tool and lock an angle per Measure a slope. Run `$T open clinometer`. `id=toolbar_title` shows the angle (`0°`) and `id=toolbar_subtitle` the slope.
- **Tilt.** Change the angle. Run `$T sensor set acceleration 0 8.5 4.9`. Set the sensor before opening: the live angle is low-pass filtered and lags after a change. With `0 8.5 4.9` the title reads `-30°` and the subtitle `-58% slope` (the sign follows the tilt direction). The `Avalanche risk` row (`data_point_desc`) is always shown; its value changes in the 30-45° band.
- **Proof.** Snap at two angles; values differ as expected.

## Gotchas

- Camera mode uses the emulated scene. Height and distance estimates need two locked angles; follow the guide's order.
- `toolbar_left_button` switches camera/side mode; `toolbar_right_button` opens the Measure (Height / Distance) picker, which shows a one-time instructions dialog.
