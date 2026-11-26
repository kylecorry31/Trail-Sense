package com.kylecorry.trail_sense.tools.navigation.domain.paths

import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PathRoute(
    private val pathPoints: List<PathPoint>,
    private val clock: Clock = Clock.systemUTC()
) {
    private val coordinates = pathPoints.map { it.coordinate }
    private val geometry = RouteGeometry(coordinates)
    private val length = geometry.length
    private val pathId = pathPoints.first().pathId
    private val cumulativeElevationLossGain = HikingService.getCumulativeElevationLossGain(pathPoints)
    private val cumulativeElevationLoss = cumulativeElevationLossGain.first
    private val cumulativeElevationGain = cumulativeElevationLossGain.second
    private val cumulativeScarfsDistance = FloatArray(coordinates.size) {
        HikingService.getScarfsDistance(
            Distance.meters(geometry.distanceAt(it)),
            Distance.meters(cumulativeElevationGain[it])
        ).meters().value
    }

    private var state = NavigationState()

    @Synchronized
    fun restorePosition(distanceAlong: Distance, location: Coordinate) {
        state = NavigationState(distanceAlong.meters().value.coerceIn(0f, length), location)
    }

    /** The guidance from the latest [navigate], which must have been called at least once. */
    val guidance: PathGuidance
        @Synchronized get() = checkNotNull(state.guidance) { "The route has not been navigated" }

    @Synchronized
    fun navigate(location: Coordinate, accuracy: Distance?): PathGuidance {
        val previousGuidance = state.guidance
        if (previousGuidance != null && location == state.location) {
            return previousGuidance
        }
        val currentTime = Instant.now(clock)
        val tolerance = (accuracy?.meters()?.value ?: 0f)
            .coerceIn(ROUTING_SAME_PLACE_TOLERANCE_METERS, CURRENT_LOCATION_MAX_TOLERANCE_METERS)
        val match = matchLocation(location, tolerance, currentTime)
        val guidance = getGuidance(location, match, tolerance)
        state.time = currentTime
        state.distanceAlong = guidance.distanceAlong.meters().value
        state.location = location
        state.guidance = guidance
        return guidance
    }

    private fun getGuidance(location: Coordinate, match: Snap, tolerance: Float): PathGuidance {
        val arrived = hasArrived(location, match.distanceAlong, tolerance)
        val remainingDistance = Distance.meters(if (arrived) 0f else match.offset + length - match.distanceAlong)
        val (remainingElevationLoss, remainingElevationGain) = if (arrived) {
            Distance.meters(0f) to Distance.meters(0f)
        } else {
            getRemainingElevationLossGain(match)
        }
        val currentElevation = elevationAt(pathPoints, match)
        val projected = geometry.coordinateOf(match)
        return PathGuidance(
            target = getTarget(match, projected),
            remainingDistance = remainingDistance,
            offRouteDistance = Distance.meters(match.offset),
            arrived = arrived,
            remainingRoute = RemainingRoute(
                buildList {
                    add(PathPoint(-1, pathId, location, currentElevation))
                    if (match.offset > OFF_ROUTE_PROJECTION_INCLUSION_DISTANCE_METERS) {
                        add(PathPoint(-2, pathId, projected, currentElevation))
                    }
                },
                pathPoints.subList(match.segment + 1, pathPoints.size)
            ),
            remainingElevationGain = remainingElevationGain,
            remainingElevationLoss = remainingElevationLoss,
            distanceAlong = Distance.meters(if (arrived) length else match.distanceAlong),
            progress = getProgress(match, arrived),
        )
    }

    private fun getRemainingElevationLossGain(currentPoint: Snap): Pair<Distance, Distance> {
        return Distance.meters(cumulativeElevationLoss.at(currentPoint) - cumulativeElevationLoss.last()) to
                Distance.meters(cumulativeElevationGain.last() - cumulativeElevationGain.at(currentPoint))
    }

    private fun getProgress(currentPoint: Snap, arrived: Boolean): Float = when {
        arrived -> 1f
        length > 0f -> (cumulativeScarfsDistance.at(currentPoint) / cumulativeScarfsDistance.last()).coerceIn(0f, 1f)
        else -> 0f
    }

    private fun FloatArray.at(snap: Snap): Float =
        Interpolation.lerp(snap.percentAlongSegment, this[snap.segment], this[snap.segment + 1])

    private fun hasArrived(location: Coordinate, distanceAlong: Float, tolerance: Float): Boolean {
        val radius = if (state.guidance?.arrived == true) tolerance * ARRIVED_RADIUS_MULTIPLIER else tolerance
        return length - distanceAlong <= radius && location.distanceTo(coordinates.last()) <= radius
    }

    private fun getTarget(current: Snap, projected: Coordinate): Coordinate {
        if (current.offset > OFF_ROUTE_TARGET_DISTANCE_METERS) {
            return projected
        }
        return geometry.pointAt(current.distanceAlong + max(TARGET_LOOKAHEAD_METERS, current.offset))
    }

    private fun matchLocation(location: Coordinate, tolerance: Float, currentTime: Instant): Snap {
        val movement = state.location?.distanceTo(location) ?: 0f
        val lastDistance = state.distanceAlong
        val secondsSinceLastUpdate = state.time?.let { Duration.between(it, currentTime).toMillis() / 1000f } ?: 0f
        val possibleHikingDistance = MAX_HIKING_SPEED_METERS_PER_SECOND * secondsSinceLastUpdate
        val movementDistance = lastDistance + movement
        val possibleTravelDistance = max(movement + tolerance, possibleHikingDistance)
        val closest = geometry.getOptimalLocationMatch(
            location,
            lastDistance - possibleTravelDistance,
            lastDistance + possibleTravelDistance,
            movementDistance
        )
        val nearby = preferLegAhead(location, closest, lastDistance, possibleTravelDistance, movementDistance)
        return rejoinRoute(location, nearby, lastDistance, movementDistance, movement, tolerance)
    }

    // WHY: Overlapping legs (ex. out and back) are indistinguishable within GPS error, so the leg ahead is preferred
    private fun preferLegAhead(
        location: Coordinate,
        closest: Snap,
        lastDistance: Float,
        possibleTravelDistance: Float,
        estimatedDistance: Float
    ): Snap {
        if (closest.distanceAlong >= lastDistance) {
            return closest
        }
        val ahead = geometry.getOptimalLocationMatch(
            location,
            lastDistance,
            lastDistance + possibleTravelDistance,
            estimatedDistance
        )
        val isLegAhead = ahead.distanceAlong > lastDistance + MIN_LEG_AHEAD_PROGRESS_METERS
        val isAboutAsClose = ahead.offset <= closest.offset + ROUTING_SAME_PLACE_TOLERANCE_METERS
        return if (isLegAhead && isAboutAsClose) ahead else closest
    }

    private fun rejoinRoute(
        location: Coordinate,
        nearby: Snap,
        lastDistance: Float,
        estimatedDistance: Float,
        movement: Float,
        tolerance: Float
    ): Snap {
        val rejoinMargin = min(tolerance, REJOIN_MAX_TOLERANCE_METERS)
        if (nearby.offset <= rejoinMargin) {
            state.pendingRejoin = null
            return nearby
        }
        val bestOverallLocation = geometry.getOptimalLocationMatch(location, 0f, length, estimatedDistance)
        val progressLost = max(lastDistance - bestOverallLocation.distanceAlong, 0f)
        if (bestOverallLocation.offset + progressLost >= nearby.offset - rejoinMargin) {
            state.pendingRejoin = null
            return nearby
        }
        val pending = state.pendingRejoin
        if (pending != null && abs(bestOverallLocation.distanceAlong - pending) <= tolerance + movement * REJOIN_MOVEMENT_MULTIPLIER) {
            state.pendingRejoin = null
            return bestOverallLocation
        }
        state.pendingRejoin = bestOverallLocation.distanceAlong
        return nearby
    }

    private class RemainingRoute(
        private val start: List<PathPoint>,
        private val rest: List<PathPoint>
    ) : AbstractList<PathPoint>() {
        override val size = start.size + rest.size

        override fun get(index: Int): PathPoint {
            return if (index < start.size) start[index] else rest[index - start.size]
        }
    }

    private class NavigationState(
        var distanceAlong: Float = 0f,
        var location: Coordinate? = null,
        var time: Instant? = null,
        var guidance: PathGuidance? = null,
        var pendingRejoin: Float? = null
    )

    private companion object {
        private const val CURRENT_LOCATION_MAX_TOLERANCE_METERS = 75f

        private const val ARRIVED_RADIUS_MULTIPLIER = 2f

        private const val REJOIN_MOVEMENT_MULTIPLIER = 2f

        private const val REJOIN_MAX_TOLERANCE_METERS = 30f

        private const val MAX_HIKING_SPEED_METERS_PER_SECOND = 2f

        private const val MIN_LEG_AHEAD_PROGRESS_METERS = 1f

        private const val TARGET_LOOKAHEAD_METERS = 25f

        private const val OFF_ROUTE_TARGET_DISTANCE_METERS = 30f

        private const val OFF_ROUTE_PROJECTION_INCLUSION_DISTANCE_METERS = 1f
    }
}
