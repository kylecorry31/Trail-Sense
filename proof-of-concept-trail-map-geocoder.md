# Proof of concept: geocoder using trail maps

Issue: kylecorry31/Trail-Sense#3730

## Question

Can Trail Sense geocode (name to coordinate) and reverse geocode (coordinate to place name) from its Mapsforge trail maps (`.map` files) fast enough for interactive use?

## Result

- Reverse geocoding: feasible. Rural lookups take about 20 ms. Dense city lookups take 0.2 to 0.7 s.
- Forward search near the user: feasible. One chunk (about 7 km) takes 0.15 to 0.3 s. 256 chunks (about the NYC metro) take 5 to 6 s.
- Forward search over a whole map: not feasible at typing speed. The file format has no name index. A full scan of a 1.3 GB map took 38.5 s on an emulator, so a phone will take minutes.
- A name index built at import time is the likely fix. It was not built or measured.

## How it works

- `readNamedItems(upperLeft, lowerRight)` on `MapFile` returns the named POIs and ways for a tile range. The app already uses it for area labels.
- Forward search:
  - Split the map bounds into 4x4 tile chunks (zoom 14).
  - Sort the chunks by distance to the user.
  - Read each chunk and keep items whose name contains the query.
  - Keep the closest item for each name and kind.
  - Sort by exact match, then prefix match, then distance.
- Reverse geocoding:
  - Read a 5x5 tile window (zoom 14) around the point.
  - Build a description from: the smallest named area that contains the point, the nearest named road within 150 m, and the nearest `place` node.
  - Example: `Adirondack Park · near Mount Marcy Trail · 5 km from Marcy Landing`.
- A way has no point, so its position is its label position, or the center of its first ring's bounding box.

## Changes

- `MapsforgeGeocoder.kt` (new, in `tools/offline_maps/infrastructure/trail_maps/mapsforge`): `search` and `reverse`.
- `CoordinateInputView.kt`: new "Search (trail maps)" entry in the autofill source picker. It asks for a query, shows the results in a list, and sets the coordinate from the chosen result.
- `LocationBottomSheet.kt` and `fragment_location.xml`: new `place_name` text view, filled by `reverse`. It re-queries after the location moves 100 m.

## Measurements

- Map: `NewYork-plus-east_oam.osm.map`, 1.3 GB, 10,086 chunks of 4x4 tiles.
- Devices: Android 7 x86 emulator (1 GB RAM, 80 MB heap, 4 host cores) and Android 16 x86_64 emulator. Both run at host CPU speed. No real phone was tested.
- Each case ran once, except reverse lookups, which ran twice (cold and warm).

| Case | Time |
|---|---|
| Reverse, rural | 6 to 27 ms |
| Reverse, dense Manhattan (5x5 tiles) | 170 to 270 ms warm, up to 740 ms first call |
| Forward, 1 chunk | 135 to 325 ms |
| Forward, 16 chunks | 1.3 to 1.9 s |
| Forward, 64 chunks | 2.8 to 3.8 s |
| Forward, 256 chunks | 5 to 6.4 s |
| Full scan, 10,086 chunks, no matches stored | 38.5 s (8.5 M raw items) |

- A 16x16 chunk in dense NYC returned about 400,000 items and crashed the process on the Android 16 emulator. Keep chunks small.
- "Albany" matched only streets from Times Square within 256 chunks. The city and Mount Marcy were never reached.
- `readNamedItems` returns many items that are not useful names, such as buildings and road segments. Results need name filtering and de-duplication.

## Hardcoded or stubbed

- If no trail map is visible, the geocoder reads `/sdcard/Android/data/com.kylecorry.trail_sense/files/ny.map`.
- Zoom level, chunk size, window radius, result limit (20), chunk limit in the UI (40), and the 100 m re-query distance are fixed.
- No debounce, cancellation, caching, or error handling.
- UI strings are hardcoded English.
- The search is a dialog, not a dropdown in the coordinate field.
- Results show raw tags such as `leisure=park`, not icons.
- Beacons, paths, and photo maps are not searched.
- Each map is searched on its own, so results from several maps are not ranked together.
- Reverse geocoding quality gaps:
  - Large park polygons were not always matched. The Central Park test point fell back to the nearest road.
  - Road distance uses vertices only.
  - The "km from place" text shows `0 km` for places under 1 km away.
