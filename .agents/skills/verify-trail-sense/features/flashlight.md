# Flashlight

Use the Flashlight tool to provide light or signal for help. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_flashlight.txt`](../../../../guides/en-US/guide_tool_flashlight.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Flashlight` → tap the tile.
- Quick action (see the guide's Quick action section).
- Home screen or in-app widgets (see the guide's Widgets section).
- Precondition shortcut only: `$T open flashlight`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **Screen flashlight.** Use the screen as a flashlight per the guide. Run `$T open flashlight`, `$T ui find Screen`. Screen turns white or red.
- **Torch.** Turn on the torch. Run `$T ui find` the toggle. Emulator has no flash unit; record whether the toggle is hidden or disabled.
- **Proof.** Snap the screen flashlight. Torch, strobe, and SOS need a real flash: report them as not verified on the emulator.

## Gotchas

- The emulator lacks a camera flash; never mark torch behavior verified from the emulator.
- Volume-button toggling: `$T ui key volume-up` delivers the key, but only meaningful with a torch.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
