package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import kotlin.math.abs

object PathRouteBuilder {
    // Locations within this distance of each other can't be told apart given typical GPS error
    private const val GPS_NOISE_TOLERANCE_METERS = 5f

    fun buildRoute(
        points: List<PathPoint>,
        location: Coordinate,
        mode: PathNavigationMode,
        destinationPointId: Long? = null
    ): List<PathPoint> {
        require(points.isNotEmpty())
        val sorted = HikingService().correctElevations(points.sortedBy { it.id })
        val coordinates = sorted.map { it.coordinate }
        val geometry = RouteGeometry(coordinates)
        if (geometry.length == 0f) return listOf(sorted.first())

        val loop = sorted.size >= 3 && isLoop(geometry.length, coordinates.first().distanceTo(coordinates.last()))
        return when {
            destinationPointId != null -> findShortestRoute(
                sorted,
                location,
                sorted.indexOfFirst { it.id == destinationPointId },
                loop,
                geometry
            )

            mode.isFullLoop -> followPath(sorted, location, mode.isReversed, geometry)
            else -> findShortestRoute(sorted, location, if (mode.isReversed) 0 else sorted.lastIndex, loop, geometry)
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

    // The route along the path from the earliest candidate snap, in the order of the path
    private fun followPath(
        points: List<PathPoint>,
        location: Coordinate,
        reversed: Boolean,
        geometry: RouteGeometry
    ): List<PathPoint> {
        val path = if (reversed) points.reversed() else points
        val pathGeometry = if (reversed) RouteGeometry(path.map { it.coordinate }) else geometry
        val snap = findCandidateSnaps(path, location, pathGeometry).first()
        return listOf(snap.point) + path.drop(snap.segment + 1)
    }

    /**
     * Builds the shortest route from the location to the point at [destinationIndex]. On a loop,
     * the route may cross the gap between the end of the path and its start. The path must have
     * a length.
     */
    private fun findShortestRoute(
        points: List<PathPoint>,
        location: Coordinate,
        destinationIndex: Int,
        loop: Boolean,
        geometry: RouteGeometry
    ): List<PathPoint> {
        require(destinationIndex in points.indices)
        val lengths = PathLengths(points.map { it.coordinate }, geometry)

        // Only the lengths of the candidates are compared, since there can be many of them and each
        // route can be as long as the whole path
        val best = findCandidateSnaps(points, location, geometry)
            .flatMap { itineraries(it, destinationIndex, points.lastIndex, loop) }
            .minBy { lengths.of(it) }
        return listOf(best.snap.point) + best.runs.flatMap { run -> run.map { points[it] } }
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
    // within the GPS error, so the closest is not necessarily the right one. The geometry must have
    // a length so there is a segment to snap to.
    private fun findCandidateSnaps(points: List<PathPoint>, location: Coordinate, geometry: RouteGeometry): Sequence<Snap> {
        val projections = geometry.projections(location)
        val coordinates = projections.map { geometry.coordinateOf(it) }
        val distances = coordinates.map { location.distanceTo(it) }
        val closest = distances.min()
        return projections.indices.asSequence()
            .filter { distances[it] <= closest + GPS_NOISE_TOLERANCE_METERS }
            .map {
                val segment = projections[it].segment
                Snap(segment, interpolate(points[segment], points[segment + 1], coordinates[it]))
            }
    }

    private fun interpolate(a: PathPoint, b: PathPoint, coordinate: Coordinate): PathPoint {
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

    private class PathLengths(private val coordinates: List<Coordinate>, private val geometry: RouteGeometry) {
        // Uses the cumulative distances so the route doesn't need to be built to measure it
        fun of(itinerary: Itinerary): Float {
            var length = 0f
            var position = itinerary.snap.point.coordinate
            for (run in itinerary.runs) {
                length += position.distanceTo(coordinates[run.first]) +
                    abs(geometry.distanceAt(run.last) - geometry.distanceAt(run.first))
                position = coordinates[run.last]
            }
            return length
        }
    }
}
