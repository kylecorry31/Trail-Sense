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

- **Slope.** Open the tool and lock an angle per Measure a slope. Run `$T open clinometer`. Angle and slope percentage shown.
- **Tilt.** Change the angle. Run `$T sensor set acceleration 0 8.5 4.9`. Angle near 30°; avalanche risk text appears in that range.
- **Proof.** Snap at two angles; values differ as expected.

## Gotchas

- Camera mode uses the emulated scene. Height and distance estimates need two locked angles; follow the guide's order.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
