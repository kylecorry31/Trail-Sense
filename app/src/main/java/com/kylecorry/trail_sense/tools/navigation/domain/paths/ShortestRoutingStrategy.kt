package com.kylecorry.trail_sense.tools.navigation.domain.paths

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import kotlin.math.abs

internal class ShortestRoutingStrategy(private val destinationIndex: Int) : RoutingStrategy {
    private val hikingService = HikingService()

    override fun buildRoute(points: List<PathPoint>, location: Coordinate): List<PathPoint> {
        val coordinates = points.map { it.coordinate }
        val geometry = RouteGeometry(coordinates)
        if (geometry.length == 0f) return listOf(points.first())
        require(destinationIndex in points.indices)
        val loop = hikingService.isLoop(coordinates)
        val best = geometry.candidateSnaps(location)
            .flatMap { candidateRoutes(it, points.lastIndex, loop) }
            .minBy { it.getLength(coordinates, geometry) }
        return listOf(PathInterpolationUtils.pointAt(points, geometry, best.snap)) + best.runs.flatMap { run -> run.map { points[it] } }
    }

    private fun candidateRoutes(snap: Snap, lastIndex: Int, loop: Boolean): List<CandidateRoute> {
        val segment = snap.segment
        val destinationAhead = destinationIndex > segment
        val onPath = if (destinationAhead) {
            listOf(segment + 1..destinationIndex)
        } else {
            listOf(segment downTo destinationIndex)
        }
        if (!loop) return listOf(CandidateRoute(snap, onPath))

        val acrossSeam = if (destinationAhead) {
            listOf(segment downTo 0, lastIndex downTo destinationIndex)
        } else {
            listOf(segment + 1..lastIndex, 0..destinationIndex)
        }
        return listOf(onPath, acrossSeam).map { CandidateRoute(snap, it) }
    }

    private class CandidateRoute(val snap: Snap, val runs: List<IntProgression>) {
        fun getLength(coordinates: List<Coordinate>, geometry: RouteGeometry): Float {
            var length = 0f
            var position = geometry.coordinateOf(snap)
            for (run in runs) {
                length += position.distanceTo(coordinates[run.first]) +
                        abs(geometry.distanceAt(run.last) - geometry.distanceAt(run.first))
                position = coordinates[run.last]
            }
            return length
        }
    }
}
