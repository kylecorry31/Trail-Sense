# White Noise

Use the White Noise tool to help you fall asleep while you camp or backpack. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_white_noise.txt`](../../../../guides/en-US/guide_tool_white_noise.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `White Noise` → tap the tile.
- Quick action (see the guide's Quick action section).
- Precondition shortcut only: `$T open white-noise`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- Notifications permission.

- **Play.** Start white noise and set a sleep timer. Run `$T open white-noise`. Playing state and a media notification.
- **Service.** Check the service is running. Run `$T adb -- shell dumpsys activity services com.kylecorry.trail_sense | grep -i noise`. Service listed.
- **Proof.** Snap the playing state; save the dumpsys excerpt. Stop playback afterwards.

## Gotchas

- No audio output on a headless emulator.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
