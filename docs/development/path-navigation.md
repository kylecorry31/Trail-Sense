# Path Navigation

Path shape:

- **Loop**: A path where the start and end points are close to each other and can be connected to form a continuous loop. It must:
  - Have at least three points.
  - Be more than 30 meters long.
  - Have a distance between the start and end points that does not exceed 10% of the total path length or 100 meters, whichever is smaller.
- **Out and back**: A path where the user follows the same route to the destination and it ends far from the starting point.

## Routing

Routing occurs when navigation to a path starts or is restored, and it determines the route the user needs to follow from their location to arrive at their destination using the selected mode.

A snap is the projection of the user's location onto a path segment. Snaps that are more than 5 meters farther from the user than the closest snap are filtered out. The elevation of a snap is linearly interpolated between the elevations of the segment's endpoints, if available.

There are two snap selection strategies:

- **Earliest snap**: Selects the snap that appears first on the ordered path.
- **Optimal snap**: Selects the snap that results in the shortest overall itinerary to the destination.

An itinerary is a possible sequence of path segments that the user will follow to reach their destination. For an out and back path, the only possible itinerary is to follow the path to the destination. For a loop path, a second possible itinerary is to cross the seam (the imaginary segment between start and end) and continue along the loop in the opposite direction.

The routing mode is based on what the user selected:

- **Follow to point**: The shortest itinerary from the optimal snap to the selected point.
- **Follow to end**: The shortest itinerary from the optimal snap to the last point.
- **Follow to start**: The shortest itinerary from the optimal snap to the first point.
- **Follow full loop**: The remaining path from the earliest snap to the last point.
- **Follow full loop in reverse**: The same as follow full loop, but the path points are reversed before calculating the earliest snap.

## Progress Tracking

Once the route is built, new locations are matched to the route to track the user's progress along it. Guidance to the next target is generated based on this progress. This happens each time the user's location changes. If the location is the same as the previous update, the previous guidance is reused.

### Location Matching

The location is projected onto the route (similar to a snap, but on the route that was built) to get the distance along the route and the offset (how far the user is from the route). To determine the best match, only the part of the route near the previous progress is considered, factoring in the user's recent movement. This prevents overlapping or nearby parts of the route (ex. the start and end of a loop) from being incorrectly matched before the user has reached them. For the first location, progress starts at the beginning of the route, so only the start of the route is searched (a user who is elsewhere is handled by the rejoin check below). If multiple candidates are within 1 meter of the closest, the furthest along the route is used.

If the match is more than 15 meters off the route, but somewhere else on the route is more than 15 meters closer, the user may have taken a shortcut and rejoined the route further along (or back). The progress only moves to the new position once it is seen (within a distance threshold) twice in a row, so a single GPS outlier can't move it. If parts of the route overlap, the earliest is used.

### Guidance

- **Arrived**: The user's progress is within 15 meters of the end of the route, and they are within 15 meters of its last point. Progress is complete, and the remaining distance and elevation are zero.
- **Remaining distance**: The offset plus the distance left along the route.
- **Remaining elevation gain and loss**: Interpolated from the cumulative gain and loss at the user's position along the route.
- **Remaining route**: The user's location, their projection onto the route (if more than 1 meter away), and the rest of the route's points.
- **Target**: The location the user should head toward.
  - If more than 30 meters off the route, the target is the projection onto the route.
  - Otherwise, it is the next sharp corner within the lookahead (the greater of 25 meters or the offset) until the user is within 8 meters of it.
  - If there is no corner, it is the point at the end of the lookahead.
  - A corner is a turn of at least 35 degrees, measured over 15 meters before and after the point so GPS jitter isn't treated as a corner. Sharp points within 15 meters of each other (ex. along a tight bend) are treated as one corner, located at the sharpest of them.

### Saving and Restoring Progress

The progress is saved each time it is updated, along with the route's inputs: the path, selected mode, destination point, and the location the route was built from. When navigation is restored (ex. after the app is restarted), the route is rebuilt from those inputs using the path's current points, and the saved progress is applied to it. Progress is saved as a distance along the route rather than a fraction of it, because the route may be longer than before (ex. an active backtrack path). The last location is saved too, so the next update measures the user's movement from it, and any pending rejoin is discarded.