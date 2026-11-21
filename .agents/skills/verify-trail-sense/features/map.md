# Map

Use the Map tool to see your beacons, paths, offline maps, and other map layers in a single place. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_map.txt`](../../../../guides/en-US/guide_tool_map.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Map` → tap the tile.
- Bottom navigation tab `Map` (default tabs).
- Home screen or in-app widgets (see the guide's Widgets section).
- Precondition shortcut only: `$T open map`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- `$T geo 42.3601 -71.0589 20`.
- A beacon nearby (see beacons.md) for Navigate to a beacon.

- **Open.** Tap the Map tab. Run `$T ui tap desc=Map`. Map centered on the GPS fix.
- **Create beacon.** Long-press the map and create a beacon per the guide. Run `$T ui hold 360 600`. Beacon form opens with that location.
- **Measure.** Measure a distance per the guide. Run `$T ui find Measure`. Distance text shown.
- **Proof.** Snap each state; for created beacons, `$T db query "select name, latitude, longitude from beacons"`.

## Gotchas

- Long press has no selector form; take coordinates from `$T ui find` and use `$T ui hold X Y`.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
