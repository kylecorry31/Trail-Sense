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

    init {
        require(coordinates.isNotEmpty())
    }

    private val hikingService = HikingService()
    private val geometry = RouteGeometry(coordinates)
    val length = geometry.length
    private val pathId = pathPoints.first().pathId
    private val cumulativeElevationLossGain = hikingService.getCumulativeElevationLossGain(pathPoints)
    private val cumulativeElevationLoss = cumulativeElevationLossGain.first
    private val cumulativeElevationGain = cumulativeElevationLossGain.second
    private val cumulativeScarfsDistance = FloatArray(coordinates.size) {
        hikingService.getScarfsDistance(
            Distance.meters(geometry.distanceAt(it)),
            Distance.meters(cumulativeElevationGain[it])
        ).meters().value
    }

    private val state = NavigationState()

    @Synchronized
    fun restoreProgress(progress: Float, location: Coordinate) {
        state.restore(progress, location)
    }

    @Synchronized
    fun navigate(location: Coordinate, accuracy: Distance?): PathGuidance {
        val previousGuidance = state.guidance
        if (previousGuidance != null && location == state.location) {
            return previousGuidance
        }
        val currentTime = Instant.now(clock)
        val tolerance = (accuracy?.meters()?.value ?: 0f)
            .coerceIn(CURRENT_LOCATION_MIN_TOLERANCE_METERS, CURRENT_LOCATION_MAX_TOLERANCE_METERS)
        val match = matchLocation(location, tolerance, currentTime)
        val guidance = getGuidance(location, match, tolerance)
        state.time = currentTime
        state.progress = guidance.progress
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
            target = getTarget(location, match, projected),
            remainingDistance = remainingDistance,
            offRouteDistance = Distance.meters(match.offset),
            arrived = arrived,
            remainingRoute = buildList {
                add(PathPoint(-1, pathId, location, currentElevation))
                if (match.offset > OFF_ROUTE_PROJECTION_INCLUSION_DISTANCE_METERS) {
                    add(PathPoint(-2, pathId, projected, currentElevation))
                }
                addAll(pathPoints.subList(min(match.segment + 1, coordinates.lastIndex), pathPoints.size))
            },
            remainingElevationGain = remainingElevationGain,
            remainingElevationLoss = remainingElevationLoss,
            progress = getProgress(match.distanceAlong, arrived),
            effortProgress = getEffortProgress(match, arrived),
        )
    }

    private fun getRemainingElevationLossGain(currentPoint: Snap): Pair<Distance, Distance> {
        return Distance.meters(cumulativeElevationLoss.last() - cumulativeElevationLoss.at(currentPoint)) to
                Distance.meters(cumulativeElevationGain.last() - cumulativeElevationGain.at(currentPoint))
    }

    private fun getProgress(distanceAlong: Float, arrived: Boolean): Float = when {
        arrived -> 1f
        length > 0f -> (distanceAlong / length).coerceIn(0f, 1f)
        else -> 0f
    }

    private fun getEffortProgress(currentPoint: Snap, arrived: Boolean): Float = when {
        arrived -> 1f
        length > 0f -> (cumulativeScarfsDistance.at(currentPoint) / cumulativeScarfsDistance.last()).coerceIn(0f, 1f)
        else -> 0f
    }

    private fun FloatArray.at(snap: Snap): Float {
        if (snap.segment >= coordinates.lastIndex) return last()
        return Interpolation.lerp(snap.percentAlongSegment, this[snap.segment], this[snap.segment + 1])
    }

    private fun hasArrived(location: Coordinate, distanceAlong: Float, tolerance: Float): Boolean {
        val radius = if (state.guidance?.arrived == true) tolerance * ARRIVED_RADIUS_MULTIPLIER else tolerance
        return length - distanceAlong <= radius && location.distanceTo(coordinates.last()) <= radius
    }

    private fun getTarget(location: Coordinate, current: Snap, projected: Coordinate): Coordinate {
        // If the user is off route, steer toward the projected point on the route instead of a corner
        if (current.offset > OFF_ROUTE_TARGET_DISTANCE_METERS) {
            return projected
        }

        // Reset the last reached corner if the user has walked back far enough
        if (current.distanceAlong < state.lastReachedCorner - TARGET_CORNER_RESET_METERS) {
            state.lastReachedCorner = Float.NEGATIVE_INFINITY
        }

        // Look ahead to the next corner on the route, but not too far ahead
        val lookahead = current.distanceAlong + max(TARGET_LOOKAHEAD_METERS, current.offset)
        for (distance in geometry.cornersBetween(current.distanceAlong, lookahead)) {
            if (distance > state.lastReachedCorner) {
                val point = geometry.pointAt(distance)
                // If the user is far enough away from the corner, target it. Otherwise, keep looking for a corner further ahead.
                if (location.distanceTo(point) > TARGET_CORNER_REACHED_METERS) {
                    return point
                }
                // The user has reached this corner
                state.lastReachedCorner = distance
            }
        }

        // If there are no corners ahead, target the point at the lookahead distance
        return geometry.pointAt(lookahead)
    }

    private fun matchLocation(location: Coordinate, tolerance: Float, currentTime: Instant): Snap {
        val movement = state.location?.distanceTo(location) ?: 0f
        val lastDistance = state.progress * length
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
        var progress: Float = 0f,
        var location: Coordinate? = null,
        var time: Instant? = null,
        var guidance: PathGuidance? = null,
        var pendingRejoin: Float? = null,
        var lastReachedCorner: Float = Float.NEGATIVE_INFINITY
    ) {
        fun restore(progress: Float, location: Coordinate) {
            this.progress = progress.coerceIn(0f, 1f)
            this.location = location
            this.time = null
            this.guidance = null
            this.pendingRejoin = null
            this.lastReachedCorner = Float.NEGATIVE_INFINITY
        }
    }

    private companion object {
        // The minimum tolerance for using the current location for matching/arrival
        private const val CURRENT_LOCATION_MIN_TOLERANCE_METERS = 15f

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

        // How far away the user must be from a corner before it is considered reached
        private const val TARGET_CORNER_REACHED_METERS = 8f

        // How far back the user must walk before the last reached corner is reset
        private const val TARGET_CORNER_RESET_METERS = 15f

        // How far off route before the projected point becomes the target
        private const val OFF_ROUTE_TARGET_DISTANCE_METERS = 30f

        // How far off route before the projected point is included in the remaining route
        private const val OFF_ROUTE_PROJECTION_INCLUSION_DISTANCE_METERS = 1f
    }
}
