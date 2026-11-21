# Magnifier

Use the Magnifier tool to magnify close-up objects and text with your phone's camera. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_magnifier.txt`](../../../../guides/en-US/guide_tool_magnifier.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Magnifier` → tap the tile.
- Precondition shortcut only: `$T open magnifier`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- `$T perm grant camera`.

- **Zoom.** Zoom in and out per the guide. Run `$T open magnifier`, `$T ui find` the zoom control. Zoom level changes.
- **Freeze.** Freeze the image. Run tap the freeze control. Image stops updating.
- **Proof.** Snap before and after zoom; the emulated scene scales.

## Gotchas

- Torch and focus mode need real camera hardware.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
