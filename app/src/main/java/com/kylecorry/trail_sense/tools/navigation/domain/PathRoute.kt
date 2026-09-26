package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import kotlin.math.abs

class PathRoute(pathPoints: List<PathPoint>) {
    private val points = pathPoints.map { it.coordinate }
    private val hikingService = HikingService()
    private val cumulativeDistances = hikingService.getDistances(points).toFloatArray()
    private val cumulativeElevationLossGain = hikingService.getCumulativeElevationLossGain(pathPoints)
    private val cumulativeElevationLoss = cumulativeElevationLossGain.first
    private val cumulativeElevationGain = cumulativeElevationLossGain.second
    private var previousProgress: Float = 0f
    private var previousLocation: Coordinate? = null
    private var previousGuidance: Guidance? = null
    var onProgressChanged: ((Float, Coordinate) -> Unit)? = null

    init {
        require(points.isNotEmpty())
    }

    @Synchronized
    fun restoreProgress(progress: Float, location: Coordinate?) {
        previousProgress = progress.coerceIn(0f, 1f)
        previousLocation = location
        previousGuidance = null
    }

    @Synchronized
    fun navigate(location: Coordinate): Guidance {
        if (location == previousLocation && previousGuidance != null) {
            return previousGuidance!!
        }
        val current = getCurrentPoint(location)
        val next = getNextPoint(location, current)
        val remainingDistance = if (next.arrived) {
            0f
        } else {
            next.offRoute + cumulativeDistances.last() - current.distance
        }
        val (remainingElevationLoss, remainingElevationGain) = if (next.arrived) {
            Distance.meters(0f) to Distance.meters(0f)
        } else {
            getRemainingElevationLossGain(current)
        }
        val guidance = Guidance(
            target = next.target,
            remainingDistance = remainingDistance,
            offRoute = next.offRoute,
            arrived = next.arrived,
            remainingRoute = listOf(location, next.target) + points.drop(next.index),
            remainingElevationGain = remainingElevationGain,
            remainingElevationLoss = remainingElevationLoss,
        )
        previousProgress = getProgress(current.distance, next.arrived)
        previousLocation = location
        previousGuidance = guidance
        onProgressChanged?.invoke(previousProgress, location)
        return guidance
    }

    private fun getRemainingElevationLossGain(currentPoint: CurrentPoint): Pair<Distance, Distance> {
        if (currentPoint.segment >= points.lastIndex) {
            return Distance.meters(0f) to Distance.meters(0f)
        }

        val segmentDistance = cumulativeDistances[currentPoint.segment + 1] - cumulativeDistances[currentPoint.segment]
        if (segmentDistance <= 0f) {
            return Distance.meters(0f) to Distance.meters(0f)
        }

        val fraction = ((currentPoint.distance - cumulativeDistances[currentPoint.segment]) / segmentDistance)
            .coerceIn(0f, 1f)
        val currentLoss = Interpolation.lerp(
            fraction,
            cumulativeElevationLoss[currentPoint.segment],
            cumulativeElevationLoss[currentPoint.segment + 1]
        )
        val currentGain = Interpolation.lerp(
            fraction,
            cumulativeElevationGain[currentPoint.segment],
            cumulativeElevationGain[currentPoint.segment + 1]
        )
        return Distance.meters(cumulativeElevationLoss.last() - currentLoss) to
                Distance.meters(cumulativeElevationGain.last() - currentGain)
    }

    private fun getProgress(distance: Float, arrived: Boolean): Float = when {
        arrived -> 1f
        cumulativeDistances.last() > 0f -> (distance / cumulativeDistances.last()).coerceIn(0f, 1f)
        else -> 0f
    }

    private fun hasArrived(location: Coordinate, distance: Float): Boolean {
        return cumulativeDistances.last() - distance <= ARRIVAL_RADIUS_METERS &&
                location.distanceTo(points.last()) <= ARRIVAL_RADIUS_METERS
    }

    private fun getNextPoint(location: Coordinate, currentPoint: CurrentPoint): NextPoint {
        var next = (currentPoint.segment + 1).coerceAtMost(points.lastIndex)
        while (next < points.lastIndex && cumulativeDistances[next] - currentPoint.distance < LOOKAHEAD_METERS) {
            next++
        }
        val offRoute = location.distanceTo(currentPoint.projected)
        val arrived = hasArrived(location, currentPoint.distance)
        val target = if (offRoute > OFF_ROUTE_DISTANCE_METERS) currentPoint.projected else points[next]
        return NextPoint(next, target, arrived, offRoute)
    }

    private fun getCurrentPoint(location: Coordinate): CurrentPoint {
        var segment = 0
        var projection = points.first()
        var distance = 0f
        var bestScore = Float.POSITIVE_INFINITY
        val movement = previousLocation?.distanceTo(location) ?: 0f
        val previousDistance = previousProgress * cumulativeDistances.last()
        for (i in 0 until points.lastIndex) {
            val lower = maxOf(cumulativeDistances[i], previousDistance - movement - PROGRESS_TOLERANCE_METERS)
            val upper = minOf(
                cumulativeDistances[i + 1],
                previousDistance + movement * FORWARD_PROGRESS_MULTIPLIER + PROGRESS_TOLERANCE_METERS
            )
            if (cumulativeDistances[i + 1] == cumulativeDistances[i] || lower > upper) {
                continue
            }
            val projected = Geography.getNearestPoint(location, points[i], points[i + 1])
            val along = (cumulativeDistances[i] + points[i].distanceTo(projected)).coerceIn(lower, upper)
            val nearest =
                points[i].plus(Distance.meters(along - cumulativeDistances[i]), points[i].bearingTo(points[i + 1]))
            val score = location.distanceTo(nearest)
            val forwardTie = abs(score - bestScore) < MATCH_TOLERANCE_METERS && along > distance
            if (score < bestScore - MATCH_TOLERANCE_METERS || forwardTie) {
                bestScore = score
                segment = i
                projection = nearest
                distance = along
            }
        }
        return CurrentPoint(segment, projection, distance)
    }

    private data class CurrentPoint(val segment: Int, val projected: Coordinate, val distance: Float)
    private data class NextPoint(val index: Int, val target: Coordinate, val arrived: Boolean, val offRoute: Float)

    data class Guidance(
        val target: Coordinate,
        val remainingDistance: Float,
        val offRoute: Float,
        val arrived: Boolean,
        val remainingRoute: List<Coordinate>,
        val remainingElevationGain: Distance,
        val remainingElevationLoss: Distance
    )

    private companion object {
        const val FORWARD_PROGRESS_MULTIPLIER = 2f
        const val PROGRESS_TOLERANCE_METERS = 15f
        const val MATCH_TOLERANCE_METERS = 1f
        const val LOOKAHEAD_METERS = 10f
        const val ARRIVAL_RADIUS_METERS = 15f
        const val OFF_ROUTE_DISTANCE_METERS = 30f
    }
}
