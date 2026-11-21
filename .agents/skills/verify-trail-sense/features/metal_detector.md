# Metal Detector

Use the Metal Detector tool to find magnetic metals, such as iron. It is useful for finding dropped keys or knives. It can't detect coins or jewelry. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_metal_detector.txt`](../../../../guides/en-US/guide_tool_metal_detector.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Metal Detector` → tap the tile.
- Precondition shortcut only: `$T open metal-detector`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- Baseline field: `$T sensor set magnetic-field 22 5 -40`.

- **Calibrate.** Open the tool and calibrate per the guide. Run `$T open metal-detector`. Reading near the baseline.
- **Detect.** Raise the field. Run `$T sensor set magnetic-field 120 80 -150`. Reading jumps and the metal indicator triggers.
- **Proof.** Snap before and after the field change.

## Gotchas

- Audio feedback can't be heard on a headless emulator started with -no-audio.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
