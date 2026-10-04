# Path Navigation
Guides the user along a saved path to a destination. Navigation has three phases:

1. **Routing**: build the route the user must follow from their current location.
2. **Progress tracking**: match each new location to the route and produce guidance.
3. **Persistence**: save and restore a navigation session across app restarts.

## Definitions

| Term | Meaning |
| --- | --- |
| Path | An ordered list of points (coordinate, optional elevation). Order is ascending point id. |
| Segment *i* | The straight line between path point *i* and point *i + 1*. |
| Seam | The imaginary segment from the last point of a path to its first point. |
| Snap | The projection of a location onto a path segment. There is at most one snap per segment. Its elevation is linearly interpolated between the segment's endpoint elevations, or absent if either is absent. |
| Route | The ordered points the user follows. The first point is always a snap or the path's only point. A snap keeps the id and path id of the path point that starts its segment. |
| Distance along | The distance from the start of the route to a position on it, measured along the route. All distances (path, route, offset, and straight-line) are horizontal geodesic distances; elevation is ignored. |
| Offset | The straight-line distance from a location to its projection onto the route. |
| Progress | Distance along of the user's matched position. Stored as a fraction of route length in memory. |

## Path Shape

A path is exactly one of:

- **Loop**: it has at least 3 points, its length is greater than 30 m, and the distance between its first and last points is at most `min(100 m, 10% of its length)`.
- **Out and back**: any other path.

Loop is determined from the path's coordinates only.

## Routing

### Inputs

| Input | Description |
| --- | --- |
| points | The path's points. Must not be empty. |
| location | The user's location when routing. |
| mode | A `PathNavigationMode` (below). |
| destinationPointId | Optional id of a path point. When present, `mode` is ignored. It must identify a point in `points`. |

### Output

The route as a list of path points. It has one point when the path has one point.

### Preparation

Before routing, sort `points` by id and correct elevations with `HikingService.correctElevations` (a point without an elevation stays without one). Corrected elevations are used for the route and all guidance.

### Modes

| Mode | Destination | Snap strategy | Itineraries considered |
| --- | --- | --- | --- |
| `TO_END` | Last point | Optimal | Shortest |
| `REVERSED_TO_END` | First point | Optimal | Shortest |
| `FULL_LOOP` | Last point | Earliest | The path forward only |
| `REVERSED_FULL_LOOP` | First point of the original path | Earliest, on the reversed path | The path forward only |
| Destination point | The given point | Optimal | Shortest |

`TO_END` is also called "follow to end". `REVERSED_TO_END` is "follow to start". A destination point is "follow to point".

The full loop modes are only offered for loop paths. A path with one point routes to that point in every mode.

### Candidate snaps

1. Compute the snap of `location` on every segment.
2. Let *closest* be the smallest distance from `location` to any snap.
3. Keep the snaps within `closest + 5 m` of the location, ordered by segment index. Overlapping parts of a path (ex. the shared start and end of a loop) produce several candidates, since they can't be told apart within GPS error.

### Snap strategies

- **Earliest**: use the first candidate snap.
- **Optimal**: use the candidate snap whose itinerary is shortest.

### Itineraries

An itinerary is a sequence of path points from a snap to the destination. Its length is the straight-line distance from the snap to its first point, plus the path distance along its points. Crossing the seam adds the straight-line distance between the last and first points.

For a snap on segment *s* and a destination at point index *d*:

- **Direct**: follow the path from the snap to *d*. This goes forward through points *s + 1 … d* if *d > s*, otherwise backward through points *s … d*.
- **Across the seam** (loops only): go the other way around the loop.
  - If *d > s*: backward through points *s … 0*, then from the last point backward through points *last … d*.
  - Otherwise: forward through points *s + 1 … last*, then through points *0 … d*.

Out and back paths only have the direct itinerary. Loop paths have both.

### Selection

- Optimal snap: evaluate every itinerary of every candidate snap and select the shortest. On a tie, the first wins. Snaps are evaluated in segment order. For a snap, if *d > s* the direct itinerary comes before the one across the seam, otherwise the one across the seam comes first.
- Full loop: route = the earliest snap, followed by every point after the snap's segment of the (possibly reversed) path.

## Progress Tracking

A route is navigated by calling `navigate(location)` for each new location. It returns **guidance**. Navigation is serialized: concurrent calls and restores do not interleave.

The route holds this state:

| State | Initial value |
| --- | --- |
| previous progress | 0 |
| previous location | none |
| previous guidance | none |
| pending rejoin | none |
| reached corner | -∞ |

### Update rule

1. If there is previous guidance and `location` equals the previous location, return that guidance. Nothing else changes and the progress listener is not called.
2. Match the location to the route (below) to get the current projection.
3. Compute the guidance (below).
4. Set previous progress to 1 if arrived, otherwise `distance along / route length` clamped to [0, 1] (0 if the route has no length).
5. Set the previous location and previous guidance.
6. Notify the progress listener with the new progress and the location.

### Location matching

Matching projects the location onto the route, giving the distance along and the offset.

1. Compute `movement`: the distance from the previous location to this location, or 0 if there is none.
2. Search only the part of the route between `previous progress distance ± (movement + 15 m)`. The first location therefore only searches the start of the route.
3. Take the closest projection in the range. Of all projections within 1 m of the closest, take the furthest along the route. If no segment is in range, use the start of the range clamped to [0, route length], with the offset measured from the location to that position.
4. Apply the rejoin check.

### Rejoin check

Detects a user who left the route and returned further along it (or back). A rejoin must be seen twice in a row, so a single GPS outlier cannot move the progress.

1. If the nearby match is at most 15 m off the route, clear the pending rejoin and use the nearby match.
2. Otherwise, search the whole route for the closest projection. Of all projections within 1 m of the closest, take the *earliest*.
3. If that projection is not more than 15 m closer than the nearby match, clear the pending rejoin and use the nearby match.
4. If there is a pending rejoin within `15 m + 2 × movement` (along the route) of that projection, clear it and use that projection.
5. Otherwise, set the pending rejoin to that projection's distance along and use the nearby match.

### Guidance

| Field | Value |
| --- | --- |
| arrived | True when the route's remaining length from the matched position is at most 15 m **and** the location is within 15 m of the route's last point. |
| remaining distance | 0 if arrived. Otherwise the offset plus the route length minus the distance along. |
| off route | The offset. |
| remaining elevation gain and loss | 0 if arrived or at the last point. Otherwise the route's total gain and loss minus the gain and loss at the matched position, interpolated between the cumulative values at the segment's endpoints. Elevation changes are summed between consecutive points: increases are gain, decreases are loss. For this sum only, a point without an elevation takes the previous point's elevation, and leading points without one take the first known elevation (all 0 if none are known), as in `HikingService.getElevations`. |
| remaining route | See below. |
| target | See below. |

**Remaining route** is, in order:

1. The user's location.
2. The projection onto the route, only if the offset is more than 1 m.
3. Every route point after the matched segment.

The location and projection have the elevation of the matched position: the endpoint's elevation if the position is exactly at a segment endpoint, otherwise interpolated between the segment's endpoints (absent if either is absent). They use placeholder ids -1 and -2 and the path's id.

**Target** is the coordinate the user should head toward. It aims at a point a short way ahead on the route, so the user converges onto the route instead of walking parallel to it.

1. If the offset is more than 30 m, the target is the projection onto the route.
2. Otherwise, set the lookahead position to `distance along + max(25 m, offset)`.
3. If the user is more than 15 m behind the reached corner, forget the reached corner.
4. For each corner between the distance along (exclusive) and the lookahead position (inclusive), nearest first, ignoring corners at or before the reached corner:
   - If the location is more than 8 m from the corner, the target is the corner.
   - Otherwise, the corner is reached: remember it and continue to the next.
5. If no corner qualifies, the target is the point on the route at the lookahead position (the end of the route if it is past the end).

### Corners

A corner is a place on the route where the direction of travel changes by at least 35°.

- The turn at a route point is the angle between the direction from the point 15 m before it (along the route) to the point, and the direction from the point to the point 15 m after it. A point 15 m before the start or after the end of the route is the start or end of the route. Measuring over a stretch stops GPS jitter on densely recorded paths from being detected as corners.
- A turn is 0° if either stretch is shorter than 3 m (ex. at the ends of the route).
- Route points with a turn of at least 35° that are within 15 m (along the route) of the previous sharp point form a single corner. The corner is located at the sharpest of them (the earliest if tied).

### Geometry

- Positions on a route are distances along it. A route with a single point is treated as a zero length segment.
- Projection onto a segment uses a flat local plane around the location.
- Segments of zero length are never projected onto. A single point route therefore never has a projection onto a segment: it always matches position 0 on its only segment, with the offset measured to the point. Its remaining route follows the usual rules: the location, the projection point (which is the point itself) if the offset is more than 1 m, then that point.

## Persistence

One navigation session is saved at a time. The saved state is:

| Key | Contents |
| --- | --- |
| Route inputs | Path id, mode, destination point id (optional), and the location the route was built from. |
| Progress | Distance along the route, and the last location. |

Progress is saved as a distance rather than a fraction because the route may be longer when restored (ex. a path currently being recorded by backtrack).

### Lifecycle

**Start**: `navigate(path, points, mode, destinationPointId)`

1. Do nothing if `points` is empty, or if `destinationPointId` is given and is not one of `points`.
2. Build the route from the current location.
3. Cancel any current navigation.
4. Save the route inputs and clear any saved progress.
5. Publish the route as the active destination.

**Progress**: every time the route notifies its progress listener (even if the progress is unchanged), save the distance along (`progress × route length`) and the location. It is only saved for the active route.

**Location changes**: while navigating, each location update calls `navigate` on the active route.

**Cancel**: stop any restore in progress, clear the saved state, and clear the active destination. Cancelling a path id only does this if the active (or saved) navigation is for that path.

**Restore** runs when the app starts, if there is a saved route.

1. Load the path and its current points. If the saved state is invalid, the path is gone, or it has no points, restoration fails.
2. Rebuild the route from the saved inputs with the path's **current** points.
3. If saved progress exists and the rebuilt route has length, restore it: progress is `saved distance / new route length` (clamped to [0, 1]), the previous location is the saved location, and previous guidance, the pending rejoin, and the reached corner are cleared.
4. Publish the route as the active destination.

If restoration fails for any reason other than cancellation, the saved state is cleared.

A restore is discarded if navigation was started or cancelled while it was running. While a saved route exists and has not finished restoring, navigation is reported as restoring.
