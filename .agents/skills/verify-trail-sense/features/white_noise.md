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

- **Play.** Start white noise and set a sleep timer. Run `$T open white-noise`; the spinner defaults to `Pink noise`. Set the sleep timer first (`$T ui tap id=sleep_timer_switch`, then its duration), because changing the sound or timer stops playback, then `$T ui tap id=white_noise_btn`. The notification is a plain persistent one ("Tap to turn off"), not a media notification.
- **Service.** Check the service is running. Run `$T adb -- shell dumpsys activity services com.kylecorry.trail_sense | grep -i noise`. `WhiteNoiseService` is listed (3 matching lines on 2026-11-21). Tap `id=white_noise_btn` again to stop.
- **Proof.** Snap the playing state; save the dumpsys excerpt. Stop playback afterwards.

## Gotchas

- No audio output on a headless emulator.
