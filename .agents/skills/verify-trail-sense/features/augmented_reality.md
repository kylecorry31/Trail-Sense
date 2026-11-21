# Augmented Reality

Use the Augmented Reality tool to see beacons, paths, and the sun, moon, stars, and planets overlaid onto the real world through your camera. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_augmented_reality.txt`](../../../../guides/en-US/guide_tool_augmented_reality.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Augmented Reality` → tap the tile.
- Precondition shortcut only: `$T open augmented-reality`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- Camera permission (`$T perm grant camera`).
- A beacon to look at: create one in Beacons first (see beacons.md).

- **Open.** Open from the Tools tab. Run `$T open augmented-reality`. Camera view with overlay; the emulator shows its virtual scene.
- **Layers.** Open the layers sheet and toggle beacons per Choose which layers are visible. Run `$T ui find layer`. Beacon markers appear or disappear.
- **Proof.** Snap with the layer on and off. Alignment against the real world can't be checked on the emulator.

## Gotchas

- Needs a compass; the emulator's magnetic sensor counts. Move the heading with `$T env sensor set magnetic-field 0 20 -40` vs `20 0 -40`.
- Calibration and Navigate with guidance can't be verified meaningfully without real sky or landmarks; report as not verified.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
