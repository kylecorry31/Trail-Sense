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

- **Open.** Tap the Map tab. Run `$T ui tap desc=Map`. The first open shows a `Map` disclaimer dialog (`text=/^OK$/`). Map controls: `menu_btn`, `time_btn`, `zoom_in_btn`, `zoom_out_btn`, `lock_btn`.
- **Create beacon.** Long-press the map and create a beacon per the guide. Run `$T ui hold 360 600` (800 ms). A sheet shows the pressed point's coordinates and elevation (`42.359669°,  -71.058899°` on 2026-11-21) with the Beacon / Navigate / Distance actions; tap `text=/^Beacon$/` for the form with that location.
- **Measure.** Measure a distance per the guide. Tap `id=menu_btn`: the popup lists `Measure`, `Create path`, `Layers` and `Trace`. Tap `text=/^Measure$/`, then tap two map points for the distance sheet.
- **Proof.** Snap each state; for created beacons, `$T db query "select name, latitude, longitude from beacons"`.

## Gotchas

- Long press has no selector form; take coordinates from `$T ui find` and use `$T ui hold X Y`.
