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
- Baseline field: `$T env sensor set magnetic-field 22 5 -40`.

- **Calibrate.** Open the tool and calibrate per the guide. Set the baseline field before opening, then run `$T open metal-detector` and wait over 2 s: the tool auto-calibrates 2 s after opening (or tap `id=calibrate_btn`), so a field injected earlier becomes the reference. `id=toolbar_title` reads `46 uT`.
- **Detect.** Raise the field. Run `$T env sensor set magnetic-field 120 80 -150`. `id=toolbar_title` reads `208 uT` and the metal icon appears as a drawable on the title (not a separate view). Driven on 2026-11-21.
- **Proof.** Snap before and after the field change.

## Gotchas

- Audio feedback can't be heard on a headless emulator started with -no-audio.
- A first-open disclaimer (`id=metal_detector_disclaimer`) is part of the layout, not a dialog.
