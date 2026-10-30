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

    private val state = NavigationState()

    @Synchronized
    fun restorePosition(distanceAlong: Float, location: Coordinate) {
        state.restore(distanceAlong.coerceIn(0f, length), location)
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
        val currentElevation = PathInterpolationUtils.elevationAt(pathPoints, match)
        val projected = geometry.coordinateOf(match)
        return PathGuidance(
            target = getTarget(match, projected),
            remainingDistance = remainingDistance,
            offRouteDistance = Distance.meters(match.offset),
            arrived = arrived,
            remainingRoute = buildList {
                add(PathPoint(-1, pathId, location, currentElevation))
                if (match.offset > OFF_ROUTE_PROJECTION_INCLUSION_DISTANCE_METERS) {
                    add(PathPoint(-2, pathId, projected, currentElevation))
                }
                addAll(pathPoints.subList(match.segment + 1, pathPoints.size))
            },
            remainingElevationGain = remainingElevationGain,
            remainingElevationLoss = remainingElevationLoss,
            distanceAlong = Distance.meters(if (arrived) length else match.distanceAlong),
            progress = getProgress(match, arrived),
        )
    }

    private fun getRemainingElevationLossGain(currentPoint: Snap): Pair<Distance, Distance> {
        return Distance.meters(cumulativeElevationLoss.last() - cumulativeElevationLoss.at(currentPoint)) to
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
        // How far the user could have hiked since the last update (likely more than actual)
        val possibleHikingDistance = MAX_HIKING_SPEED_METERS_PER_SECOND * secondsSinceLastUpdate
        // How far the user could have moved based on the GPS change (likely less than actual)
        val movementDistance = lastDistance + movement
        // The likely maximum for how far the user could have moved along the route since the last update
        val possibleTravelDistance = max(movement + tolerance, possibleHikingDistance)
        val nearby = geometry.getOptimalLocationMatch(
            location,
            lastDistance - possibleTravelDistance,
            lastDistance + possibleTravelDistance,
            movementDistance
        )
        return rejoinRoute(location, nearby, lastDistance, movementDistance, movement, tolerance)
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
        // If the user is close enough to the nearby match, use it instead of rejoining
        if (nearby.offset <= rejoinMargin) {
            state.pendingRejoin = null
            return nearby
        }
        val bestOverallLocation = geometry.getOptimalLocationMatch(location, 0f, length, estimatedDistance)
        val progressLost = max(lastDistance - bestOverallLocation.distanceAlong, 0f)
        // If accepting the nearby match is better than rejoining somewhere else, use it instead of rejoining
        if (bestOverallLocation.offset + progressLost >= nearby.offset - rejoinMargin) {
            state.pendingRejoin = null
            return nearby
        }
        val pending = state.pendingRejoin
        // If the user has a pending rejoin, and it is likely the same as the best overall location, rejoin there
        if (pending != null && abs(bestOverallLocation.distanceAlong - pending) <= tolerance + movement * REJOIN_MOVEMENT_MULTIPLIER) {
            state.pendingRejoin = null
            return bestOverallLocation
        }
        // Otherwise, mark the intent to rejoin and wait for the next update to confirm it
        state.pendingRejoin = bestOverallLocation.distanceAlong
        return nearby
    }

    private data class NavigationState(
        var distanceAlong: Float = 0f,
        var location: Coordinate? = null,
        var time: Instant? = null,
        var guidance: PathGuidance? = null,
        var pendingRejoin: Float? = null
    ) {
        fun restore(distanceAlong: Float, location: Coordinate) {
            this.distanceAlong = distanceAlong
            this.location = location
            this.time = null
            this.guidance = null
            this.pendingRejoin = null
        }
    }

    private companion object {
        // The maximum tolerance for using the current location for matching/arrival
        private const val CURRENT_LOCATION_MAX_TOLERANCE_METERS = 75f

        // How much larger the arrival radius is after the user has already arrived to prevent back and forth
        private const val ARRIVED_RADIUS_MULTIPLIER = 2f

        // How much the movement since the last update is multiplied by to determine if a rejoin is likely
        private const val REJOIN_MOVEMENT_MULTIPLIER = 2f

        // The cap for rejoin tolerance
        private const val REJOIN_MAX_TOLERANCE_METERS = 30f

        // Speed used to estimate how much of the path the user could have hiked for location matching
        private const val MAX_HIKING_SPEED_METERS_PER_SECOND = 2f

        // How far ahead the user should look for a target point
        private const val TARGET_LOOKAHEAD_METERS = 25f

        // How far off route before the projected point becomes the target
        private const val OFF_ROUTE_TARGET_DISTANCE_METERS = 30f

        // How far off route before the projected point is included in the remaining route
        private const val OFF_ROUTE_PROJECTION_INCLUSION_DISTANCE_METERS = 1f
    }
}
