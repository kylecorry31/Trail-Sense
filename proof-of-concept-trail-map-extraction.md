# Proof of concept: extract paths and beacons from trail maps

Issue: https://github.com/kylecorry31/Trail-Sense/issues/3723

## Result

The feature is feasible. The app can make a full trail from a mapsforge map when the user long-presses near the trail. It can also make a beacon from a named map feature.

## Changes

### `MapsforgeFeatureExtractor.kt` (new)

Path: `app/src/main/java/com/kylecorry/trail_sense/tools/offline_maps/infrastructure/trail_maps/mapsforge/`

- `extractPath(location)` makes one line for the trail nearest to the location.
  1. Read the zoom 14 tiles around the location. Find the nearest way that has a `highway` tag. The maximum distance is 50 m. This way is the seed.
  2. Get the group key of the seed. Other pieces of the trail must have the same key.
  3. Find the connected pieces. Read the tiles around the open ends of each new piece. Add each piece with the same key that touches a known piece. Stop when no new pieces are found, or when the tile limit (400) is reached.
  4. Stitch the pieces into one line. Extend one end, then reverse the line and extend the other end.
- `extractPoi(location)` returns the nearest POI that has a `name` tag. The maximum distance is 100 m. It also returns the `ele` tag if there is one.
- `withVisibleMaps { }` opens all visible trail maps as one `MultiMapDataStore` and closes them after the block.

### `MapFragment.kt` (changed)

Path: `app/src/main/java/com/kylecorry/trail_sense/tools/map/ui/`

- The long-press menu has two new actions:
  - **Path from map** calls `createPathFromTrailMap`. It extracts the path, saves it with `CreatePathCommand`, and opens the path details. A toast shows the point count, piece count, tile count, and time. The user can skip the elevation lookup.
  - **Beacon from map** calls `createBeaconFromTrailMap`. It opens the beacon form with the name, location, and elevation filled in. The data goes through the `label` and `ele` query parameters of `GeoUri`.

### `PathExtractionProbeTest.kt` (new)

Path: `app/src/test/java/com/kylecorry/trail_sense/tools/offline_maps/infrastructure/trail_maps/mapsforge/`

- This test is a measurement tool, not a unit test. It reads a local map file and prints results.
- It writes GeoJSON for each case to `.scratch/poc-paths/`.

## Mapsforge data facts

- Mapsforge files do not keep OSM way IDs.
- One trail is many ways. The tags change between ways, for example `surface` and `trail_visibility`.
- The writer clips ways at tile borders. The pieces in adjacent tiles overlap by approximately 20 m. They do not meet end to end. Thus the stitcher looks for the end of the line on any part of a piece and adds only the part after it (the "tail").
- OpenAndroMaps adds route tags to ways: `ref_hike`, `osmc_color`, and `hknetwork`. If two routes share a way, the way keeps only one `ref_hike`.

## Group key

The group key controls where the trail stops.

- By name (`name`, then `ref_hike`, then `ref`): the trail stops where the way name changes.
- By route (`ref_hike`, then `ref`, then `name`): the trail continues through name changes. This is the default.
- A way with none of these tags joins only with ways that have exactly the same tags.

## Measurements

Device: desktop JVM, 1 thread. Map: `NewYork-plus-east_oam.osm.map` (OpenAndroMaps, 1.3 GB). Each case ran 2 times. The table shows the second run.

| Location | Group key | Pieces | Length | Time |
|---|---|---|---|---|
| Van Hoevenberg Trail | name | 16 | 7.7 km | 15 ms |
| Van Hoevenberg Trail | route | 41 | 11.4 km (published: approximately 11.9 km) | 13 ms |
| Cornell Mine Trail | route | 11 | 3.9 km | 6 ms |
| Washington Av, Albany | name | 62 | 8.1 km | 67 ms |
| ECT/AT, 250 tile limit | route | 333 | 174 km | 1.0 s |
| ECT/AT, no limit | route | 1544 | 996 km | 19 s |
| POI lookup | - | - | - | 1 to 25 ms |

The maximum gap between joined pieces was 1.7 m.

## Known problems

- Long routes are slow. The tile limit cuts the route, but the cut is not centered on the location. Stitching is O(n²) in the number of pieces.
- When a route branches (spurs), the stitcher keeps one branch only.
- Divided roads (two one-way ways) do not stitch.
- The `ref_hike` route key is specific to OpenAndroMaps. Other map sources need a different key.

## Hardcoded or not done

- Zoom 14, a 3 m join tolerance, a 50 m path search distance, a 100 m POI search distance, and a 400 tile limit.
- UI labels are not string resources. The actions show when no trail maps are visible.
- No check for duplicate paths. No cancel for a slow extraction. No elevation from the map.
- The test has hardcoded local file paths.
- Tested on the desktop JVM. The emulator has the APK and the map file, but no one has used the actions on a device yet.
