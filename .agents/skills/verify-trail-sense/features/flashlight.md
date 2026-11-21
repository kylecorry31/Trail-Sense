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

- **Screen flashlight.** Use the screen as a flashlight per the guide. Run `$T open flashlight`, `$T ui tap id=screen_flashlight_btn`. The screen turns white; `id=off_btn` (`Turn off`) ends it and `id=red_white_switcher` switches red/white.
- **Torch.** Turn on the torch. The Android_16 emulator exposes a torch: `id=flashlight_on_btn` and `id=brightness_seek` are shown and the tap works (`dumpsys media.camera` lists torch strength levels).
- **Proof.** Snap the screen flashlight. Torch, strobe, and SOS need a real flash: report them as not verified on the emulator.

## Gotchas

- The torch toggle works on the emulator but no light output can be observed; never mark torch, strobe or SOS behavior verified from it.
- Volume-button toggling: `$T ui key volume-up` delivers the key, but only meaningful with a torch.
