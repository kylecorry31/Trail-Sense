# Declination

Use the Declination tool to find the declination to set on your physical compass. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_declination.txt`](../../../../guides/en-US/guide_tool_declination.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Declination` → tap the tile.
- Precondition shortcut only: `$T open declination`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- `$T env geo 42.3601 -71.0589 20`.

- **Find.** Open the tool. Run `$T open declination`. Declination for the location (about -14° in Boston).
- **Proof.** Snap the value; change the location with `$T env geo` and confirm it changes.

## Gotchas

- Adjusting the compass for declination is a Settings pref; check it with `$T pref list declination`.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
