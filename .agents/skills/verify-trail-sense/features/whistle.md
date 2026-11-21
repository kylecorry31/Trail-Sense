# Whistle

Use the Whistle tool to signal for help. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_whistle.txt`](../../../../guides/en-US/guide_tool_whistle.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Whistle` → tap the tile.
- Quick action (see the guide's Quick action section).
- Precondition shortcut only: `$T open whistle`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **Whistle.** Press and hold the whistle, then try SOS. Run `$T open whistle`, `$T ui tap id=whistle_sos_btn` (toggle; tap again to stop). The tiles are `whistle_btn`, `whistle_sos_btn` and `whistle_emergency_btn`; the playing state is only a tint change, so prove it with a screenshot, not the layout JSON. `whistle_btn` plays only while touched, so a plain tap flips it on and off instantly.
- **Proof.** Snap while playing. Sound can't be heard on a headless emulator; report audio as not verified.

## Gotchas

- `tsctl emu start` passes -no-audio.
