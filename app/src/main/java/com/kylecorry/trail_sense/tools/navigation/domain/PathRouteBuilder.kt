package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathSimplificationQuality
import com.kylecorry.trail_sense.tools.paths.domain.PathSimplifier
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService

object PathRouteBuilder {
    fun prepare(
        points: List<PathPoint>,
        location: Coordinate,
        mode: PathNavigationMode,
        destinationPointId: Long? = null
    ): List<PathPoint> {
        val sorted = points.sortedBy { it.id }
        val simplified = PathSimplifier.simplify(sorted, PathSimplificationQuality.High).toSet()
        val ordered = HikingService().correctElevations(
            sorted.filter { it in simplified || it.id == destinationPointId }
        )
        return if (destinationPointId == null) {
            build(ordered, location, mode)
        } else {
            toPoint(ordered, location, ordered.indexOfFirst { it.id == destinationPointId })
        }
    }

    fun isLoop(points: List<Coordinate>): Boolean {
        val length = Geography.getPathDistance(points).meters().value
        return points.size >= 3 && length > 30 &&
            points.first().distanceTo(points.last()) <= minOf(100.0, length * 0.1)
    }

    fun build(points: List<PathPoint>, location: Coordinate, mode: PathNavigationMode): List<PathPoint> {
        require(points.isNotEmpty())
        val ordered = if (mode.isReversed) points.reversed() else points
        val loop = mode.isFullLoop
        if (ordered.size == 1) return ordered
        val join = nearest(ordered, location)
        return if (loop) {
            listOf(join.point) + ordered.drop(join.segment + 1) +
                ordered.take(join.segment + 1) + join.point
        } else {
            listOf(join.point) + ordered.drop(join.segment + 1)
        }
    }

    fun toPoint(points: List<PathPoint>, location: Coordinate, destinationIndex: Int): List<PathPoint> {
        require(destinationIndex in points.indices)
        if (points.size == 1) return points
        val joins = joins(points, location)
        val distance = joins.minOf { it.distance }
        val loop = isLoop(points.map { it.coordinate })
        return joins.asSequence().filter { it.distance <= distance + 1f }
            .map { toPoint(points, it, destinationIndex, loop) }
            .minBy { length(it) }
    }

    private fun toPoint(points: List<PathPoint>, join: Join, destinationIndex: Int, loop: Boolean): List<PathPoint> {
        val destinationAhead = destinationIndex > join.segment
        val direct = listOf(join.point) + if (destinationAhead) {
            points.subList(join.segment + 1, destinationIndex + 1)
        } else {
            points.subList(destinationIndex, join.segment + 1).reversed()
        }
        if (!loop) return direct
        val wrapped = listOf(join.point) + if (destinationAhead) {
            points.take(join.segment + 1).reversed() +
                points.drop(destinationIndex).reversed()
        } else {
            points.drop(join.segment + 1) + points.take(destinationIndex + 1)
        }
        val candidates = if (destinationAhead) listOf(direct, wrapped) else listOf(wrapped, direct)
        return candidates.minBy { length(it) }
    }

    private fun length(points: List<PathPoint>): Float =
        Geography.getPathDistance(points.map { it.coordinate }).meters().value

    private fun nearest(points: List<PathPoint>, location: Coordinate): Join {
        return joins(points, location).minBy { it.distance }
    }

    private fun joins(points: List<PathPoint>, location: Coordinate): List<Join> =
        points.zipWithNext().mapIndexed { index, (a, b) ->
            val coordinate = Geography.getNearestPoint(location, a.coordinate, b.coordinate)
            Join(index, interpolate(a, b, coordinate), location.distanceTo(coordinate))
        }

    fun interpolate(a: PathPoint, b: PathPoint, coordinate: Coordinate): PathPoint {
        val distance = a.coordinate.distanceTo(b.coordinate)
        val fraction = if (distance > 0f) (a.coordinate.distanceTo(coordinate) / distance).coerceIn(0f, 1f) else 0f
        val elevation = if (a.elevation != null && b.elevation != null) {
            a.elevation + (b.elevation - a.elevation) * fraction
        } else null
        return a.copy(coordinate = coordinate, elevation = elevation)
    }

    private data class Join(val segment: Int, val point: PathPoint, val distance: Float)
}
