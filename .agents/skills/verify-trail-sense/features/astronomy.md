# Astronomy

Use the Astronomy tool to see when the sun and moon are up and when astronomical events, such as meteor showers and eclipses, happen at your location. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_astronomy.txt`](../../../../guides/en-US/guide_tool_astronomy.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Astronomy` → tap the tile.
- Bottom navigation tab `Astronomy` (default tabs).
- Quick action (see the guide's Quick action section).
- Home screen or in-app widgets (see the guide's Widgets section).
- Precondition shortcut only: `$T open astronomy`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- `$T geo 42.3601 -71.0589 20` so sun and moon times are for a known place.

- **Open.** Tap the Astronomy tab. Run `$T ui tap desc=Astronomy`. Toolbar shows sun and moon rise/set times for today.
- **Change date.** Use the date picker in the toolbar per the guide's Choose a date section. Run `$T ui find date`, then `$T ui tap id=<reported id>`. The times and the chart update for the chosen date.
- **Proof.** Snap the screen before and after changing the date; the sunrise text differs. Alerts: `$T pref list sunset` shows the alert pref after toggling it in the UI.

## Gotchas

- Times use the emulator time zone (`$T adb -- shell getprop persist.sys.timezone`), not the coordinates' zone.
- Alert sections need the notifications permission and the exact-alarm appop (`seed` grants both).
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
