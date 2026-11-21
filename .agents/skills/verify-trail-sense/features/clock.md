# Clock

Use the Clock tool to find the current time from GPS satellites and to check how accurate your device's clock is. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_clock.txt`](../../../../guides/en-US/guide_tool_clock.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Clock` → tap the tile.
- Precondition shortcut only: `$T open clock`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- Location permission and a GPS fix (`$T geo ...`).

- **GPS time.** Open the tool. Run `$T open clock`. Shows the time and the offset from GPS time.
- **Digital.** Switch to the digital clock per the guide. Run `$T ui find Digital`. Digital display.
- **Proof.** Snap both clock styles.

## Gotchas

- Syncing opens the system date settings; the app can't set the clock itself. Emulator GPS time comes from the host.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
