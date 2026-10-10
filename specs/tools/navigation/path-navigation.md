# Path Navigation

All distances (path, route, offset, straight-line) are horizontal geodesic distances, elevation is ignored. A position on a route is a distance along it. Projection onto a segment uses a flat local plane around the location, and segments of zero length are never projected onto.

Terms:
- segment i: the line between path point i and point i + 1
- snap: the projection of a location onto a segment (at most one per segment), elevation interpolated between the segment's endpoints, or absent if either is absent. A snap keeps the id and path id of the point that starts its segment
- route: the ordered points the user follows, starting with a snap (or the path's only point)
- `distanceAlong`: distance from the start of the route to a position on it, measured along the route
- `offset`: straight-line distance from a location to its projection onto the route

## Path shape

- Function: `isLoop`
- Inputs: list of coordinates (the path points' coordinates)
- Output: boolean

```
pathLength = sum of distances between consecutive points
seam = distance(first point, last point)

return points.size >= 3 and pathLength > 30 m and seam <= min(100 m, 10% of pathLength)
```

## Routing

- Object: `PathRouteBuilder`. `buildRoute` and `isLoop` are public, `findCandidateSnaps` and `findShortestRoute` are private.
- Enum: `PathNavigationMode` (`TO_END`, `REVERSED_TO_END`, `FULL_LOOP`, `REVERSED_FULL_LOOP`)

- Function: `buildRoute`
- Inputs: `points` (not empty), `location`, `mode: PathNavigationMode`, optional `destinationPointId` (must be one of the points)
- Output: list of path points

```
points = HikingService.correctElevations(points sorted by id ascending)

if points.size is 1 or every segment has zero length
    return [first point]

loop = isLoop(points)

if destinationPointId is present
    return findShortestRoute(points, location, index of destination in points, loop)

if mode is TO_END
    return findShortestRoute(points, location, last index, loop)

if mode is REVERSED_TO_END
    return findShortestRoute(points, location, 0, loop)

path = points, or points reversed if mode is REVERSED_FULL_LOOP
snap = findCandidateSnaps(path, location).first
return [snap] + path points after snap's segment
```

- Function: `findCandidateSnaps`
- Inputs: `points`, `location`
- Output: snaps ordered by segment index

```
snaps = snap of location on every segment
closestDistance = smallest distance from location to any snap

return snaps with distance to location <= closestDistance + 5 m, ordered by segment index
```

- Function: `findShortestRoute`
- Inputs: `points`, `location`, `destinationIndex`, `loop`
- Output: list of path points

```
bestItinerary = none

for snap in findCandidateSnaps(points, location)
    segmentIndex = snap's segment index

    direct = [snap] + (points segmentIndex + 1 up to destinationIndex if destinationIndex > segmentIndex, otherwise points segmentIndex down to destinationIndex)

    if loop and destinationIndex > segmentIndex
        acrossSeam = [snap] + points segmentIndex down to 0 + points last down to destinationIndex
        itineraries = [direct, acrossSeam]
    else if loop
        acrossSeam = [snap] + points segmentIndex + 1 up to last + points 0 up to destinationIndex
        itineraries = [acrossSeam, direct]
    else
        itineraries = [direct]

    update bestItinerary if any itinerary in itineraries has length shorter than bestItinerary or it is not set

return bestItinerary
```

## Progress tracking

- Class: `PathRoute(pathPoints: List<PathPoint>)` (not empty). `routeLength` is its public `length`, and the path's id is the first point's path id.
- Property: `onProgressChanged: ((progress: Float, location: Coordinate) -> Unit)?`, null by default
- Method: `navigate(location, accuracy: Distance?)`: `accuracy` is the horizontal accuracy of the location, null if unknown
- Output: `PathRoute.Guidance`
- Calls are serialized

```
Guidance
    target: coordinate
    remainingDistance: Distance
    offRoute: Distance
    arrived: boolean
    remainingRoute: list of path points
    remainingElevationGain: Distance
    remainingElevationLoss: Distance, zero or negative
    progress: Float (0 to 1)
    effortProgress: Float (0 to 1)
```

State:

```
previousProgress = 0
previousLocation = none
previousGuidance = none
pendingRejoin = none
reachedCorner = -infinity
```

```
if previousGuidance exists and location is previousLocation
    return previousGuidance

tolerance = clamp(accuracy if not null, otherwise 0 m, 15 m, 75 m)
match = matchLocation(location, tolerance)
guidance = getGuidance(location, match, tolerance)

previousLocation = location
previousGuidance = guidance
previousProgress = guidance.progress
call onProgressChanged(previousProgress, location) if it is set

return guidance
```

### Location matching

- Function: `matchLocation`
- Inputs: `location`, `tolerance: Distance`
- Output: match (the projection onto the route, with its `distanceAlong` and `offset`)

```
movement = distance(previousLocation, location), or 0 if there is no previousLocation
center = previousProgress * routeLength
range = [center - (movement + tolerance), center + (movement + tolerance)]

nearby = findClosestInRange(location, range)

rejoinMargin = min(tolerance, 30 m)

if nearby.offset <= rejoinMargin
    pendingRejoin = none
    return nearby

anywhere = findClosestInRange(location, [0, routeLength], preferNear = center)
progressLost = max(center - anywhere.distanceAlong, 0)

if anywhere.offset + progressLost >= nearby.offset - rejoinMargin
    pendingRejoin = none
    return nearby

if pendingRejoin exists and |pendingRejoin - anywhere.distanceAlong| <= tolerance + 2 * movement
    pendingRejoin = none
    return anywhere

pendingRejoin = anywhere.distanceAlong
return nearby
```

- Function: `findClosestInRange`
- Inputs: `location`, `range` (distances along the route), `preferNear` (a distance along the route, optional)
- Output: match

```
projections = projections of location onto the part of the route inside range, one per segment

if projections is empty
    position = clamp(range start, 0, routeLength)
    return position with offset = distance(location, point at position)

closestOffset = smallest offset of projections
closest = projections within 1 m of closestOffset

if preferNear is set
    return the projection of closest whose distanceAlong is nearest to preferNear (the earliest if tied)

return the furthest along the route of closest (projections are ordered by segment index, so further along means later in the route)
```

A route with zero length (a single point, or points at the same position) is one zero length segment: it always matches position 0 with the offset measured to the first point, and the remaining route includes the points after the first (the point itself if there is only one).

### Guidance

- Function: `getGuidance`
- Inputs: `location`, `match`, `tolerance: Distance`
- Output: guidance

```
remainingLength = routeLength - match.distanceAlong
arrivalRadius = 2 * tolerance if previousGuidance exists and previousGuidance.arrived, otherwise tolerance
arrived = remainingLength <= arrivalRadius and distance(location, last route point) <= arrivalRadius

if arrived
    remainingDistance = 0
else
    remainingDistance = match.offset + remainingLength

offRoute = match.offset

if arrived or match is at the last point
    remainingGain = 0
    remainingLoss = 0
else
    remainingGain = total gain of route - gain up to the matched position
    remainingLoss = total loss of route - loss up to the matched position

positionElevation = elevation at the matched position (absent if unknown)

locationPoint = location with positionElevation, id -1, the path's id
projectionPoint = match projection with positionElevation, id -2, the path's id

remainingRoute = [locationPoint]
if match.offset > 1 m
    remainingRoute += projectionPoint
remainingRoute += route points from the end of the matched segment onward

if arrived
    progress = 1
else if routeLength is 0
    progress = 0
else
    progress = clamp(match.distanceAlong / routeLength, 0, 1)

if arrived
    effortProgress = 1
else if routeLength is 0
    effortProgress = 0
else
    currentEffort = total scarf's distance along the route up to the matched position
    effortProgress = clamp(currentEffort / total scarf's distance along the route, 0, 1)

return Guidance(target = getTarget(location, match), remainingDistance, offRoute, arrived, remainingRoute, remainingElevationGain = remainingGain, remainingElevationLoss = remainingLoss, progress = progress, effortProgress = effortProgress)
```

Elevation gain and loss are summed between consecutive route points (increases are gain, decreases are loss, which is negative), and interpolated within the matched segment. For this sum only, a point without an elevation takes the previous point's elevation, and leading points without one take the first known elevation (all 0 if none are known), as in `HikingService.getElevations`. The elevation at the matched position is the endpoint's if exactly at a segment endpoint, otherwise interpolated between the segment's endpoints (absent if either is absent).

### Target

- Function: `getTarget`
- Inputs: `location`, `match`
- Output: the coordinate the user should head toward

```
if match.offset > 30 m
    return projection onto the route

lookahead = match.distanceAlong + max(25 m, match.offset)

if match.distanceAlong < reachedCorner - 15 m
    reachedCorner = -infinity

for corner in findCorners(route) between match.distanceAlong (exclusive) and lookahead (inclusive), nearest first
    if corner.distanceAlong <= reachedCorner
        continue

    if distance(location, corner) > 8 m
        return corner

    reachedCorner = corner.distanceAlong

return point on the route at min(lookahead, routeLength)
```

### Corners

- Function: `findCorners`
- Inputs: `route`
- Output: list of route points, one per corner

```
for point in route points
    before = point on route at max(point.distanceAlong - 15 m, 0)
    after = point on route at min(point.distanceAlong + 15 m, routeLength)

    if distance(before, point) < 3 m or distance(point, after) < 3 m
        turn = 0
    else
        turn = angle between direction(before, point) and direction(point, after)

sharpPoints = route points with turn >= 35 degrees, in route order
groups = []

for point in sharpPoints
    if groups is not empty and point.distanceAlong - previousSharpPoint.distanceAlong <= 15 m
        add point to last group
    else
        add new group containing point

return for each group, the point with the largest turn (the earliest if tied)
```
