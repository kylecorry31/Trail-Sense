package com.kylecorry.trail_sense.tools.navigation.domain.paths

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.PathDirection
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import kotlin.math.abs

object PathRouteBuilder {

    fun buildRoute(
        points: List<PathPoint>,
        location: Coordinate,
        follow: PathFollow
    ): List<PathPoint>? {
        if (points.isEmpty()) return null
        val reversed = follow is PathFollow.Direction && follow.direction == PathDirection.ToStart
        val path = if (reversed) points.reversed() else points
        val geometry = RouteGeometry(path.map { it.coordinate })
        if (geometry.length == 0f) return null

        return when (follow) {
            is PathFollow.Direction -> followInOrder(path, geometry, location)
            is PathFollow.ToPoint -> {
                val destinationIndex = path.indexOfFirst { it.id == follow.pointId }
                if (destinationIndex == -1) return null
                followToPoint(path, geometry, location, destinationIndex)
            }
        }
    }

    private fun followInOrder(path: List<PathPoint>, geometry: RouteGeometry, location: Coordinate): List<PathPoint> {
        val candidates = geometry.candidateSnaps(location)
        val longestRemaining = candidates.maxOf { geometry.length - it.distanceAlong }
        // Skip candidates that would leave only a sliver of the path, ex. the end of a loop when standing at its start
        val snap = candidates
            .filter { geometry.length - it.distanceAlong >= longestRemaining * MIN_REMAINING_FRACTION }
            .minBy { it.offset }
        return listOf(PathInterpolationUtils.pointAt(path, geometry, snap)) + path.drop(snap.segment + 1)
    }

    private fun followToPoint(
        path: List<PathPoint>,
        geometry: RouteGeometry,
        location: Coordinate,
        destinationIndex: Int
    ): List<PathPoint> {
        val snap = geometry.candidateSnaps(location).minBy {
            lengthTo(path, geometry, it, indicesTo(it, destinationIndex))
        }
        return listOf(PathInterpolationUtils.pointAt(path, geometry, snap)) +
                indicesTo(snap, destinationIndex).map { path[it] }
    }

    private fun indicesTo(snap: Snap, destinationIndex: Int): IntProgression {
        return if (destinationIndex > snap.segment) {
            snap.segment + 1..destinationIndex
        } else {
            snap.segment downTo destinationIndex
        }
    }

    private fun lengthTo(path: List<PathPoint>, geometry: RouteGeometry, snap: Snap, indices: IntProgression): Float {
        return geometry.coordinateOf(snap).distanceTo(path[indices.first].coordinate) +
                abs(geometry.distanceAt(indices.last) - geometry.distanceAt(indices.first))
    }

    private const val MIN_REMAINING_FRACTION = 0.25f
}
