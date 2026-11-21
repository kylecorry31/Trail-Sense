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

- **GPS time.** Open the tool. Run `$T open clock`. `id=clock_source` reads `Device time` and then `GPS time` once the single GPS fix arrives, and `id=clock_offset` reads e.g. `Device clock is accurate` (host-dependent; don't assert a value). `id=pip_button` (`Update system time`) appears after the fix; `toolbar_right_button` restarts the fix.
- **Digital.** Switch to the digital clock per the guide. There is no "Digital" control: the style is the `Analog clock` switch in the Clock tool's settings. Turn it off there (or `$T pref set @pref_enable_analog_clock false` as a precondition), reopen, and `id=digital_clock` shows.
- **Proof.** Snap both clock styles.

## Gotchas

- Syncing opens the system date settings; the app can't set the clock itself. Emulator GPS time comes from the host.
