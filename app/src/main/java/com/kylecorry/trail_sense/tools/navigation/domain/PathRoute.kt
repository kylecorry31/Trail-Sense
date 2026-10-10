package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PathRoute(private val pathPoints: List<PathPoint>) {
    private val points = pathPoints.map { it.coordinate }

    init {
        require(points.isNotEmpty())
    }

    private val hikingService = HikingService()
    private val geometry = RouteGeometry(points)
    val length = geometry.length
    private val cumulativeElevationLossGain = hikingService.getCumulativeElevationLossGain(pathPoints)
    private val cumulativeElevationLoss = cumulativeElevationLossGain.first
    private val cumulativeElevationGain = cumulativeElevationLossGain.second
    private val cumulativeScarfsDistance = FloatArray(points.size) {
        geometry.distanceAt(it) + HikingService.SCARF_ELEVATION_GAIN_FACTOR * cumulativeElevationGain[it]
    }
    private var previousProgress: Float = 0f
    private var previousLocation: Coordinate? = null
    private var previousGuidance: Guidance? = null
    private var pendingRejoin: Float? = null
    private var reachedCorner: Float = Float.NEGATIVE_INFINITY
    var onProgressChanged: ((progress: Float, location: Coordinate) -> Unit)? = null

    @Synchronized
    fun restoreProgress(progress: Float, location: Coordinate) {
        previousProgress = progress.coerceIn(0f, 1f)
        previousLocation = location
        previousGuidance = null
        pendingRejoin = null
        reachedCorner = Float.NEGATIVE_INFINITY
    }

    @Synchronized
    fun navigate(location: Coordinate, accuracy: Distance?): Guidance {
        val cached = previousGuidance
        if (cached != null && location == previousLocation) {
            return cached
        }
        val match = matchLocation(location, accuracy)
        val guidance = getGuidance(location, match)
        previousProgress = guidance.progress
        previousLocation = location
        previousGuidance = guidance
        onProgressChanged?.invoke(previousProgress, location)
        return guidance
    }

    private fun getGuidance(location: Coordinate, match: RouteProjection): Guidance {
        val arrived = hasArrived(location, match.distance)
        val remainingDistance = Distance.meters(if (arrived) 0f else match.offset + length - match.distance)
        val (remainingElevationLoss, remainingElevationGain) = if (arrived) {
            Distance.meters(0f) to Distance.meters(0f)
        } else {
            getRemainingElevationLossGain(match)
        }
        val currentElevation = getElevation(match)
        val pathId = pathPoints.first().pathId
        val projected = geometry.coordinateOf(match)
        return Guidance(
            target = getTarget(location, match, projected),
            remainingDistance = remainingDistance,
            offRoute = Distance.meters(match.offset),
            arrived = arrived,
            remainingRoute = buildList {
                add(PathPoint(-1, pathId, location, currentElevation))
                if (match.offset > SNAPPED_POINT_MIN_OFFSET_METERS) {
                    add(PathPoint(-2, pathId, projected, currentElevation))
                }
                addAll(pathPoints.subList(min(match.segment + 1, points.lastIndex), pathPoints.size))
            },
            remainingElevationGain = remainingElevationGain,
            remainingElevationLoss = remainingElevationLoss,
            progress = getProgress(match.distance, arrived),
            effortProgress = getEffortProgress(match, arrived),
        )
    }

    private fun getRemainingElevationLossGain(currentPoint: RouteProjection): Pair<Distance, Distance> {
        if (currentPoint.segment >= points.lastIndex) {
            return Distance.meters(0f) to Distance.meters(0f)
        }

        val currentLoss = Interpolation.lerp(
            currentPoint.fraction,
            cumulativeElevationLoss[currentPoint.segment],
            cumulativeElevationLoss[currentPoint.segment + 1]
        )
        val currentGain = Interpolation.lerp(
            currentPoint.fraction,
            cumulativeElevationGain[currentPoint.segment],
            cumulativeElevationGain[currentPoint.segment + 1]
        )
        return Distance.meters(cumulativeElevationLoss.last() - currentLoss) to
                Distance.meters(cumulativeElevationGain.last() - currentGain)
    }

    private fun getElevation(currentPoint: RouteProjection): Float? {
        val index = currentPoint.segment
        if (index >= points.lastIndex) return pathPoints[index].elevation
        if (currentPoint.fraction == 0f) return pathPoints[index].elevation
        if (currentPoint.fraction == 1f) return pathPoints[index + 1].elevation
        return Interpolation.lerp(
            currentPoint.fraction,
            pathPoints[index].elevation ?: return null,
            pathPoints[index + 1].elevation ?: return null
        )
    }

    private fun getProgress(distance: Float, arrived: Boolean): Float = when {
        arrived -> 1f
        length > 0f -> (distance / length).coerceIn(0f, 1f)
        else -> 0f
    }

    private fun getEffortProgress(currentPoint: RouteProjection, arrived: Boolean): Float {
        if (arrived) return 1f
        if (length <= 0f) return 0f
        val total = cumulativeScarfsDistance.last()
        val current = if (currentPoint.segment >= points.lastIndex) {
            total
        } else {
            Interpolation.lerp(
                currentPoint.fraction,
                cumulativeScarfsDistance[currentPoint.segment],
                cumulativeScarfsDistance[currentPoint.segment + 1]
            )
        }
        return (current / total).coerceIn(0f, 1f)
    }

    private fun hasArrived(location: Coordinate, distance: Float): Boolean {
        return length - distance <= ARRIVAL_RADIUS_METERS &&
                location.distanceTo(points.last()) <= ARRIVAL_RADIUS_METERS
    }

    // Steers toward the next sharp corner until it is reached, otherwise toward a point a short way
    // ahead on the route. Aiming at the route ahead rather than a distant point lets the user
    // converge onto the route instead of walking parallel to it.
    private fun getTarget(location: Coordinate, current: RouteProjection, projected: Coordinate): Coordinate {
        if (current.offset > OFF_ROUTE_DISTANCE_METERS) {
            return projected
        }
        if (current.distance < reachedCorner - CORNER_RESET_METERS) {
            reachedCorner = Float.NEGATIVE_INFINITY
        }
        val lookahead = current.distance + max(LOOKAHEAD_METERS, current.offset)
        for (distance in geometry.cornersBetween(current.distance, lookahead)) {
            if (distance > reachedCorner) {
                val point = geometry.pointAt(distance)
                if (location.distanceTo(point) > CORNER_REACHED_METERS) {
                    return point
                }
                reachedCorner = distance
            }
        }
        return geometry.pointAt(lookahead)
    }

    // Matches the location to the route. This only considers the part of the route near the
    // previous progress (the start of the route for the first location), so overlapping or nearby
    // parts of the route can't steal the match.
    private fun matchLocation(location: Coordinate, accuracy: Distance?): RouteProjection {
        val movement = previousLocation?.distanceTo(location, highAccuracy = false) ?: 0f
        val tolerance = (accuracy?.meters()?.value ?: 0f)
            .coerceIn(PROGRESS_TOLERANCE_METERS, MAX_PROGRESS_TOLERANCE_METERS)
        val previousDistance = previousProgress * length
        val nearby = geometry.findClosestInRange(
            location,
            previousDistance - movement - tolerance,
            previousDistance + movement + tolerance
        )
        return rejoinRoute(location, nearby, movement)
    }

    // If the nearby match is much worse than somewhere else on the route (ex. the user took a
    // shortcut), the user has rejoined the route elsewhere. The new position must be seen twice
    // in a row so a single GPS outlier can't move the progress. Where parts of the route overlap
    // (ex. the start and end of a loop), the earliest is used so the user isn't sent to the end.
    private fun rejoinRoute(location: Coordinate, nearby: RouteProjection, movement: Float): RouteProjection {
        if (nearby.offset <= REJOIN_MARGIN_METERS) {
            pendingRejoin = null
            return nearby
        }
        val anywhere = geometry.findClosestInRange(location, 0f, length, preferLater = false)
        if (anywhere.offset + REJOIN_MARGIN_METERS >= nearby.offset) {
            pendingRejoin = null
            return nearby
        }
        val pending = pendingRejoin
        if (pending != null && abs(anywhere.distance - pending) <= PROGRESS_TOLERANCE_METERS + movement * FORWARD_PROGRESS_MULTIPLIER) {
            pendingRejoin = null
            return anywhere
        }
        pendingRejoin = anywhere.distance
        return nearby
    }

    data class Guidance(
        val target: Coordinate,
        val remainingDistance: Distance,
        val offRoute: Distance,
        val arrived: Boolean,
        val remainingRoute: List<PathPoint>,
        val remainingElevationGain: Distance,
        val remainingElevationLoss: Distance,
        val progress: Float,
        val effortProgress: Float
    )

    private companion object {
        const val FORWARD_PROGRESS_MULTIPLIER = 2f
        const val PROGRESS_TOLERANCE_METERS = 15f
        const val MAX_PROGRESS_TOLERANCE_METERS = 75f
        const val REJOIN_MARGIN_METERS = 15f
        const val LOOKAHEAD_METERS = 25f
        const val CORNER_REACHED_METERS = 8f
        const val CORNER_RESET_METERS = 15f
        const val ARRIVAL_RADIUS_METERS = 15f
        const val OFF_ROUTE_DISTANCE_METERS = 30f
        const val SNAPPED_POINT_MIN_OFFSET_METERS = 1f
    }
}
