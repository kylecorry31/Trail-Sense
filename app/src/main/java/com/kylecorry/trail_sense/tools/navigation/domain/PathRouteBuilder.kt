package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import kotlin.math.abs

object PathRouteBuilder {
    // Locations within this distance of each other can't be told apart given typical GPS error
    private const val GPS_NOISE_TOLERANCE_METERS = 5f

    fun prepare(
        points: List<PathPoint>,
        location: Coordinate,
        mode: PathNavigationMode,
        destinationPointId: Long? = null
    ): List<PathPoint> {
        val ordered = HikingService().correctElevations(points.sortedBy { it.id })
        return if (destinationPointId == null) {
            build(ordered, location, mode)
        } else {
            toPoint(ordered, location, ordered.indexOfFirst { it.id == destinationPointId })
        }
    }

    fun isLoop(points: List<Coordinate>): Boolean {
        return points.size >= 3 && isLoop(
            Geography.getPathDistance(points).meters().value,
            points.first().distanceTo(points.last())
        )
    }

    private fun isLoop(length: Float, seam: Float): Boolean {
        return length > 30 && seam <= minOf(100f, length * 0.1f)
    }

    /**
     * Builds the route from the location to the end of the path (or to its start if the mode is
     * reversed).
     *
     * Normally this is the shortest route. In the full loop modes it is instead the way along the
     * path, even if going the other way around the loop would be shorter. If the location is on a
     * part of the path that overlaps another (ex. the shared start and end of a loop), it is
     * treated as being on the earliest one.
     */
    fun build(points: List<PathPoint>, location: Coordinate, mode: PathNavigationMode): List<PathPoint> {
        require(points.isNotEmpty())
        if (!mode.isFullLoop) {
            return toPoint(points, location, if (mode.isReversed) 0 else points.lastIndex)
        }
        val ordered = if (mode.isReversed) points.reversed() else points
        if (ordered.size == 1) return ordered
        val snap = nearSnaps(ordered, location).first()
        return listOf(snap.point) + ordered.drop(snap.segment + 1)
    }

    /**
     * Builds the shortest route from the location to the point at [destinationIndex]. On a loop,
     * the route may cross the gap between the end of the path and its start.
     */
    fun toPoint(points: List<PathPoint>, location: Coordinate, destinationIndex: Int): List<PathPoint> {
        require(destinationIndex in points.indices)
        if (points.size == 1) return points
        val lengths = PathLengths(points.map { it.coordinate })
        val loop = points.size >= 3 && isLoop(lengths.total, lengths.seam)

        // Only the lengths of the candidates are compared, since there can be many of them and each
        // route can be as long as the whole path
        val shortest = nearSnaps(points, location)
            .flatMap { itineraries(it, destinationIndex, points.lastIndex, loop) }
            .minBy { lengths.of(it) }
        return listOf(shortest.snap.point) + shortest.runs.flatMap { run -> run.map { points[it] } }
    }

    // The ways to get from the snap to the destination. When equally long, the first is preferred.
    private fun itineraries(snap: Snap, destination: Int, lastIndex: Int, loop: Boolean): List<Itinerary> {
        val segment = snap.segment
        val destinationAhead = destination > segment
        val direct = if (destinationAhead) {
            listOf(segment + 1..destination)
        } else {
            listOf(segment downTo destination)
        }
        if (!loop) return listOf(Itinerary(snap, direct))

        val acrossSeam = if (destinationAhead) {
            listOf(segment downTo 0, lastIndex downTo destination)
        } else {
            listOf(segment + 1..lastIndex, 0..destination)
        }
        return (if (destinationAhead) listOf(direct, acrossSeam) else listOf(acrossSeam, direct))
            .map { Itinerary(snap, it) }
    }

    // The places on the path that are about as close as the closest one, earliest first.
    // Overlapping parts of a path (ex. the start and end of an out and back) are indistinguishable
    // within the GPS error, so the closest is not necessarily the right one.
    private fun nearSnaps(points: List<PathPoint>, location: Coordinate): Sequence<Snap> {
        val nearest = points.zipWithNext { a, b -> Geography.getNearestPoint(location, a.coordinate, b.coordinate) }
        val distances = nearest.map { location.distanceTo(it) }
        val closest = distances.min()
        return nearest.indices.asSequence()
            .filter { distances[it] <= closest + GPS_NOISE_TOLERANCE_METERS }
            .map { Snap(it, interpolate(points[it], points[it + 1], nearest[it])) }
    }

    fun interpolate(a: PathPoint, b: PathPoint, coordinate: Coordinate): PathPoint {
        val distance = a.coordinate.distanceTo(b.coordinate)
        val fraction = if (distance > 0f) (a.coordinate.distanceTo(coordinate) / distance).coerceIn(0f, 1f) else 0f
        val elevation = if (a.elevation != null && b.elevation != null) {
            a.elevation + (b.elevation - a.elevation) * fraction
        } else null
        return a.copy(coordinate = coordinate, elevation = elevation)
    }

    /** A location on the path, which is [point] on the segment starting at the path point [segment]. */
    private class Snap(val segment: Int, val point: PathPoint)

    /**
     * A route that goes from the snap to the first index of each run, then along the path through
     * the indices of the run, and continues with the next run.
     */
    private class Itinerary(val snap: Snap, val runs: List<IntProgression>)

    private class PathLengths(private val coordinates: List<Coordinate>) {
        private val cumulative = HikingService().getDistances(coordinates).toFloatArray()
        val total = cumulative.last()

        /** The distance between the end of the path and its start */
        val seam = coordinates.last().distanceTo(coordinates.first())

        // Uses the cumulative distances so the route doesn't need to be built to measure it
        fun of(itinerary: Itinerary): Float {
            var length = 0f
            var position = itinerary.snap.point.coordinate
            for (run in itinerary.runs) {
                length += position.distanceTo(coordinates[run.first]) +
                    abs(cumulative[run.last] - cumulative[run.first])
                position = coordinates[run.last]
            }
            return length
        }
    }
}
