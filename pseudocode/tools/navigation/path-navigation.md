# Path Navigation

Level: medium

All distances (path, route, offset, straight-line) are horizontal geodesic distances, elevation is ignored. A position on a route is a distance along it. Projection onto a segment uses a flat local plane around the location, and segments of zero length are never projected onto.

Terms:
- segment i: the line between path point i and point i + 1
- snap: the projection of a location onto a segment (at most one per segment), elevation interpolated between the segment's endpoints, or absent if either is absent. A snap keeps the id and path id of the point that starts its segment
- route: the ordered points the user follows, starting with a snap (or the path's only point)
- `distanceAlong`: distance from the start of the route to a position on it, measured along the route
- `offset`: straight-line distance from a location to its projection onto the route

## Constants

| Name | Value | Used for |
| --- | --- | --- |
| `LOOP_MIN_LENGTH` | 30 m | shortest path that can be a loop |
| `LOOP_MAX_SEAM` | 100 m | largest gap between a loop's first and last point |
| `LOOP_MAX_SEAM_FRACTION` | 10% | largest seam as a fraction of the path length |
| `GPS_NOISE_TOLERANCE_METERS` | 5 m | how much farther than the closest snap a candidate snap can be |
| `MIN_TOLERANCE_METERS` | 15 m | smallest location tolerance |
| `MAX_TOLERANCE_METERS` | 75 m | largest location tolerance |
| `MAX_REJOIN_MARGIN_METERS` | 30 m | cap on how close to the route counts as on it |
| `FORWARD_PROGRESS_MULTIPLIER` | 2 | weight of the movement between locations when confirming a rejoin |
| `ARRIVED_RADIUS_MULTIPLIER` | 2 | arrival radius, as a multiple of the tolerance, once arrived |
| `SNAPPED_POINT_MIN_OFFSET_METERS` | 1 m | smallest offset that adds the projection to the remaining route |
| `OFF_ROUTE_DISTANCE_METERS` | 30 m | offset beyond which the target is the projection onto the route |
| `LOOKAHEAD_METERS` | 25 m | smallest distance ahead of the match to look for corners |
| `CORNER_REACHED_METERS` | 8 m | distance from a corner at which it counts as reached |
| `CORNER_RESET_METERS` | 15 m | how far back from the last reached corner progress resets it |
| `CORNER_CHORD_METERS` | 15 m | distance before and after a point used to measure its turn, and largest gap between sharp points in the same corner |
| `MIN_CORNER_CHORD_METERS` | 3 m | shortest before or after distance for a turn to be measured |
| `CORNER_TURN_DEGREES` | 35 degrees | smallest turn that is a corner |
| `ESTIMATE_DISTANCE_WEIGHT` | 0.01 | meters of offset that each meter between a match and the estimated distance costs, so the estimate only decides between matches that are about equally close |
| `MAX_ESTIMATE_DISTANCE_PENALTY_METERS` | 5 m | most that being far from the estimated distance can add to a match's cost, so a far-off match that is clearly closer to the location still wins |
| `NORMAL_HIKING_SPEED` | 2 m/s | typical hiking speed used for estimating distance over time |

## Routing

- Object: `PathRouteBuilder`
- Enum: `PathNavigationMode` (`TO_END`, `REVERSED_TO_END`, `FULL_LOOP`, `REVERSED_FULL_LOOP`)

### Path shape

- Public function: `isLoop`
- Inputs: list of coordinates (the path points' coordinates)
- Output: boolean

```
pathLength = sum of distances between consecutive points
seam = distance(first point, last point)

return points.size >= 3 and pathLength > LOOP_MIN_LENGTH and seam <= min(LOOP_MAX_SEAM, LOOP_MAX_SEAM_FRACTION * pathLength)
```

### Build route

- Public function: `buildRoute`
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

#### Candidate snaps

- Private function: `findCandidateSnaps`
- Inputs: `points`, `location`
- Output: snaps ordered by segment index

```
snaps = snap of location on every segment
closestDistance = smallest distance from location to any snap

return snaps with distance to location <= closestDistance + GPS_NOISE_TOLERANCE_METERS, ordered by segment index
```

#### Shortest route

- Private function: `findShortestRoute`
- Inputs: `points`, `location`, `destinationIndex`, `loop`
- Output: list of path points

For each candidate snap, the candidate routes are:
- direct: from the snap along the path to the destination
- across the seam (loops only): from the snap along the path in the other direction, through the seam (between the last and first points), to the destination

Return the shortest candidate route over all snaps. On a tie, prefer the earlier snap, then the route that travels in increasing point order.

## Progress tracking

- Class: `PathRoute(pathPoints: List<PathPoint>)` (not empty). `routeLength` is its public `length`, and the path's id is the first point's path id.
- Property: `onProgressChanged: ((progress: Float, location: Coordinate) -> Unit)?`, null by default
- Calls to `navigate` and `restoreProgress` are serialized

State:

```
previousProgress = 0
previousLocation = none
previousGuidance = none
pendingRejoin = none
reachedCorner = -infinity
previousTime = none
```

### Navigate

- Public function: `navigate`
- Inputs: `location`, `accuracy: Distance?` (the horizontal accuracy of the location, null if unknown)
- Output: `PathRoute.Guidance`

```
if previousGuidance exists and location is previousLocation
    return previousGuidance

currentTime = now()
tolerance = clamp(accuracy if not null, otherwise 0 m, MIN_TOLERANCE_METERS, MAX_TOLERANCE_METERS)
match = matchLocation(location, tolerance, currentTime)
guidance = getGuidance(location, match, tolerance)

previousTime = currentTime
previousLocation = location
previousGuidance = guidance
previousProgress = guidance.progress
call onProgressChanged(previousProgress, location) if it is set

return guidance
```

### Restore progress

- Public function: `restoreProgress`
- Inputs: `progress: Float`, `location: Coordinate` (a saved progress and location to resume the route from)
- Output: none. Does not call `onProgressChanged`

```
previousProgress = clamp(progress, 0, 1)
previousLocation = location
previousGuidance = none
pendingRejoin = none
reachedCorner = -infinity
previousTime = none
```

### Location matching

- Private function: `matchLocation`
- Inputs: `location`, `tolerance: Distance`, `currentTime: Instant`
- Output: match (the projection onto the route, with its `distanceAlong` and `offset`)

```
movement = distance(previousLocation, location), or 0 if there is no previousLocation
lastDistance = previousProgress * routeLength
dt = currentTime - previousTime if previousTime exists, otherwise 0
estimatedDistance = lastDistance + movement
possibleTravelDistance = max(movement + tolerance, NORMAL_HIKING_SPEED * dt)
range = [lastDistance - possibleTravelDistance, lastDistance + possibleTravelDistance]

nearby = getOptimalLocationMatch(location, range, estimatedDistance)

rejoinMargin = min(tolerance, MAX_REJOIN_MARGIN_METERS)

if nearby.offset <= rejoinMargin
    pendingRejoin = none
    return nearby

anywhere = getOptimalLocationMatch(location, [0, routeLength], estimatedDistance)
progressLost = max(lastDistance - anywhere.distanceAlong, 0)

if anywhere.offset + progressLost >= nearby.offset - rejoinMargin
    pendingRejoin = none
    return nearby

if pendingRejoin exists and |pendingRejoin - anywhere.distanceAlong| <= tolerance + FORWARD_PROGRESS_MULTIPLIER * movement
    pendingRejoin = none
    return anywhere

pendingRejoin = anywhere.distanceAlong
return nearby
```

#### Closest in range

- Internal function: `getOptimalLocationMatch`
- Inputs: `location`, `range` (distances along the route), `estimatedDistance` (a distance along the route)
- Output: match

```
projections = projections of location onto the part of the route inside range, one per segment

if projections is empty
    position = clamp(range start, 0, routeLength)
    return position with offset = distance(location, point at position)

costs = [projection offset + min(ESTIMATE_DISTANCE_WEIGHT * |projection distanceAlong - estimatedDistance|, MAX_ESTIMATE_DISTANCE_PENALTY_METERS) for each projection]
return the projection with the smallest cost, earliest if tied
```

A route with zero length (a single point, or points at the same position) is one zero length segment: it always matches position 0 with the offset measured to the first point, and the remaining route includes the points after the first (the point itself if there is only one).

### Guidance

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

- Private function: `getGuidance`
- Inputs: `location`, `match`, `tolerance: Distance`
- Output: guidance

```
remainingLength = routeLength - match.distanceAlong
arrivalRadius = ARRIVED_RADIUS_MULTIPLIER * tolerance if previousGuidance exists and previousGuidance.arrived, otherwise tolerance
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
if match.offset > SNAPPED_POINT_MIN_OFFSET_METERS
    remainingRoute += projectionPoint
remainingRoute += route points from the end of the matched segment onward

if arrived
    progress = 1
    effortProgress = 1
else if routeLength is 0
    progress = 0
    effortProgress = 0
else
    progress = clamp(match.distanceAlong / routeLength, 0, 1)
    effortProgress = clamp(scarf's distance along the route up to the matched position / total scarf's distance along the route, 0, 1)

return Guidance(target = getTarget(location, match), remainingDistance, offRoute, arrived, remainingRoute, remainingElevationGain = remainingGain, remainingElevationLoss = remainingLoss, progress = progress, effortProgress = effortProgress)
```

Elevation gain and loss are summed between consecutive route points (increases are gain, decreases are loss, which is negative), and interpolated within the matched segment. For this sum only, a point without an elevation takes the previous point's elevation, and leading points without one take the first known elevation (all 0 if none are known), as in `HikingService.getElevations`. The elevation at the matched position is the endpoint's if exactly at a segment endpoint, otherwise interpolated between the segment's endpoints (absent if either is absent).

#### Target

- Private function: `getTarget`
- Inputs: `location`, `match`
- Output: the coordinate the user should head toward

```
if match.offset > OFF_ROUTE_DISTANCE_METERS
    return projection onto the route

lookahead = match.distanceAlong + max(LOOKAHEAD_METERS, match.offset)

if match.distanceAlong < reachedCorner - CORNER_RESET_METERS
    reachedCorner = -infinity

for corner in findCorners(route) between match.distanceAlong (exclusive) and lookahead (inclusive), nearest first
    if corner.distanceAlong <= reachedCorner
        continue

    if distance(location, corner) > CORNER_REACHED_METERS
        return corner

    reachedCorner = corner.distanceAlong

return point on the route at min(lookahead, routeLength)
```

##### Corners

- Private function: `findCorners`
- Inputs: `route`
- Output: list of route points, one per corner

```
for point in route points
    before = point on route at max(point.distanceAlong - CORNER_CHORD_METERS, 0)
    after = point on route at min(point.distanceAlong + CORNER_CHORD_METERS, routeLength)

    if distance(before, point) < MIN_CORNER_CHORD_METERS or distance(point, after) < MIN_CORNER_CHORD_METERS
        turn = 0
    else
        turn = angle between direction(before, point) and direction(point, after)

sharpPoints = route points with turn >= CORNER_TURN_DEGREES, in route order
groups = []

for point in sharpPoints
    if groups is not empty and point.distanceAlong - previous sharp point's distanceAlong <= CORNER_CHORD_METERS
        add point to last group
    else
        add new group containing point

return for each group, the point with the largest turn (the earliest if tied)
```
