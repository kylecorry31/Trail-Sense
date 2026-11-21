# Turn Back

Use the Turn Back tool to get an alert that tells you when to turn back on a hike so you return at a specific time. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_turn_back.txt`](../../../../guides/en-US/guide_tool_turn_back.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Turn Back` → tap the tile.
- Precondition shortcut only: `$T open turn-back`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- Notifications permission and exact-alarm appop (`seed`).

- **Set time.** Set a return time per the guide. Run `$T open turn-back`. The return time and turn-back time are shown.
- **Alarm.** Check the alarm is scheduled. Run `$T adb -- shell dumpsys alarm | grep trail_sense`. An alarm for the package exists.
- **Proof.** Snap the screen; save the dumpsys alarm excerpt. Cancel the alert afterwards.

## Gotchas

- `scripts/debugging/list-scheduled-alarms.sh` shows the same data; tsctl's escape hatch keeps the target pinned.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
