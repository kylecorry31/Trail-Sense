# Mirror Camera

Use the Mirror Camera tool as a mirror, for example when you get something in your eye. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_mirror_camera.txt`](../../../../guides/en-US/guide_tool_mirror_camera.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Mirror Camera` → tap the tile.
- Precondition shortcut only: `$T open mirror-camera`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- `$T perm grant camera`.

- **Mirror.** Open the tool. Run `$T open mirror-camera`. Front camera preview (emulated scene).
- **Proof.** Snap the preview.

## Gotchas

- The emulator's front camera is emulated; only that the preview runs can be proven.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
