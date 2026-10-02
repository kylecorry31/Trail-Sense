package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min

/**
 * A point on the route closest to a location.
 * @param segment the index of the route point that starts the segment containing the projection
 * @param fraction how far along the segment the projection is, from 0 to 1
 * @param distance the distance along the route to the projection
 * @param offset the distance from the queried location to the projection
 */
internal class RouteProjection(
    val segment: Int,
    val fraction: Float,
    val distance: Float,
    val offset: Float
)

/**
 * Spatial queries over a polyline, with positions given as distances along it.
 */
internal class RouteGeometry(points: List<Coordinate>) {
    // A single point is treated as a zero length segment so every route has at least one segment
    private val routePoints = if (points.size == 1) points + points else points
    private val latitudes = DoubleArray(routePoints.size) { routePoints[it].latitude }
    private val longitudes = DoubleArray(routePoints.size) { routePoints[it].longitude }
    private val cumulativeDistances = HikingService().getDistances(routePoints).toFloatArray()
    private val lastSegment = routePoints.lastIndex - 1
    val length: Float = cumulativeDistances.last()
    private val cornerDistances = findCorners()

    /**
     * Finds the closest point on the route to the location, only considering the part of the route
     * between [minDistance] and [maxDistance]. Candidates almost as close as the closest one are
     * considered equally good, and the furthest along the route of them is used (or the earliest if
     * not [preferLater]). If there is no segment in that range, the start of the range is returned.
     */
    fun nearest(
        location: Coordinate,
        minDistance: Float,
        maxDistance: Float,
        preferLater: Boolean = true
    ): RouteProjection {
        val plane = LocalPlane(location)
        var chosen: RouteProjection? = null
        var closest = Float.POSITIVE_INFINITY
        for (segment in segmentContaining(minDistance)..segmentContaining(maxDistance)) {
            val candidate = projectOnto(segment, plane, minDistance, maxDistance) ?: continue
            closest = min(closest, candidate.offset)
            if (isBetterMatch(candidate, chosen, closest, preferLater)) {
                chosen = candidate
            }
        }
        return chosen ?: projectionAt(location, minDistance)
    }

    fun coordinateOf(projection: RouteProjection): Coordinate {
        return interpolate(projection.segment, projection.fraction)
    }

    fun pointAt(distance: Float): Coordinate {
        val segment = segmentContaining(distance)
        return interpolate(segment, fractionAlong(segment, distance))
    }

    /** The distances along the route of the sharp turns that are after [from] and up to [to]. */
    fun cornersBetween(from: Float, to: Float): List<Float> {
        return cornerDistances.subList(firstCornerAfter(from), firstCornerAfter(to))
    }

    // Segments are compared in route order, so a later candidate is further along the route
    private fun isBetterMatch(
        candidate: RouteProjection,
        chosen: RouteProjection?,
        closest: Float,
        preferLater: Boolean
    ): Boolean {
        val tolerated = closest + MATCH_TOLERANCE_METERS
        return candidate.offset <= tolerated && (preferLater || chosen == null || chosen.offset > tolerated)
    }

    // Projects the plane's origin onto the part of the segment between minDistance and maxDistance
    private fun projectOnto(
        segment: Int,
        plane: LocalPlane,
        minDistance: Float,
        maxDistance: Float
    ): RouteProjection? {
        val overlapsRange = cumulativeDistances[segment + 1] >= minDistance &&
            cumulativeDistances[segment] <= maxDistance
        if (segmentLength(segment) <= 0f || !overlapsRange) return null
        val a = plane.toMeters(latitudes[segment], longitudes[segment])
        val b = plane.toMeters(latitudes[segment + 1], longitudes[segment + 1])
        val fraction = closestFractionToOrigin(a, b)
            .coerceIn(fractionAlong(segment, minDistance), fractionAlong(segment, maxDistance))
        val projected = a + (b - a) * fraction
        return RouteProjection(
            segment,
            fraction,
            cumulativeDistances[segment] + fraction * segmentLength(segment),
            projected.magnitude()
        )
    }

    // How far along the line from a to b the closest point to the origin is
    private fun closestFractionToOrigin(a: Vector2, b: Vector2): Float {
        val ab = b - a
        val squaredLength = ab.squaredMagnitude()
        if (squaredLength == 0f) return 0f
        return -(a.x * ab.x + a.y * ab.y) / squaredLength
    }

    private fun projectionAt(location: Coordinate, distance: Float): RouteProjection {
        val clamped = distance.coerceIn(0f, length)
        val segment = segmentContaining(clamped)
        val fraction = fractionAlong(segment, clamped)
        return RouteProjection(segment, fraction, clamped, location.distanceTo(interpolate(segment, fraction)))
    }

    // The first segment that ends at or after the distance. The last segment is never searched
    // since it is the fallback.
    private fun segmentContaining(distance: Float): Int {
        return firstIndex(lastSegment) { cumulativeDistances[it + 1] >= distance }
    }

    private fun segmentLength(segment: Int): Float {
        return cumulativeDistances[segment + 1] - cumulativeDistances[segment]
    }

    private fun fractionAlong(segment: Int, distance: Float): Float {
        val segmentLength = segmentLength(segment)
        if (segmentLength <= 0f) return 0f
        return ((distance - cumulativeDistances[segment]) / segmentLength).coerceIn(0f, 1f)
    }

    private fun interpolate(segment: Int, fraction: Float): Coordinate {
        val latitude = Interpolation.lerp(fraction.toDouble(), latitudes[segment], latitudes[segment + 1])
        val longitudeChange = Coordinate.toLongitude(longitudes[segment + 1] - longitudes[segment])
        return Coordinate(latitude, longitudes[segment] + fraction * longitudeChange)
    }

    private fun firstCornerAfter(distance: Float): Int {
        return firstIndex(cornerDistances.size) { cornerDistances[it] > distance }
    }

    // A corner is where the direction of travel, measured over a short stretch before and after a
    // point, changes sharply. Measuring over a stretch rather than between adjacent points keeps
    // GPS jitter in densely recorded paths from looking like corners. Sharp points close together
    // (ex. along a tight bend) are one corner, located at the sharpest of them.
    private fun findCorners(): List<Float> {
        val turns = routePoints.indices.map(::turnAt)
        val corners = mutableListOf<MutableList<Int>>()
        for (point in routePoints.indices.filter { turns[it] >= CORNER_TURN_DEGREES }) {
            val corner = corners.lastOrNull()
            if (corner != null && cumulativeDistances[point] - cumulativeDistances[corner.last()] <= CORNER_CHORD_METERS) {
                corner.add(point)
            } else {
                corners.add(mutableListOf(point))
            }
        }
        return corners.map { corner -> cumulativeDistances[corner.maxBy { turns[it] }] }
    }

    // The change in direction, in degrees, between arriving at and leaving the point
    private fun turnAt(index: Int): Float {
        val distance = cumulativeDistances[index]
        val plane = LocalPlane(Coordinate(latitudes[index], longitudes[index]))
        val incoming = Vector2.zero - plane.toMeters(pointAt(distance - CORNER_CHORD_METERS))
        val outgoing = plane.toMeters(pointAt(distance + CORNER_CHORD_METERS))
        if (incoming.magnitude() < MIN_CORNER_CHORD_METERS || outgoing.magnitude() < MIN_CORNER_CHORD_METERS) {
            return 0f
        }
        return abs(Trigonometry.deltaAngle(incoming.angle(), outgoing.angle()))
    }

    // Binary search for the first index below count where the condition holds (or count if none),
    // given the condition is false for all indices before it and true for all after
    private inline fun firstIndex(count: Int, condition: (Int) -> Boolean): Int {
        var low = 0
        var high = count
        while (low < high) {
            val mid = (low + high) ushr 1
            if (condition(mid)) high = mid else low = mid + 1
        }
        return low
    }

    /**
     * A flat approximation of the area around [origin] in meters east (x) and north (y) of it. It is
     * accurate over the distances that route segments span, and much cheaper than geodesic math.
     */
    private class LocalPlane(origin: Coordinate) {
        private val originLatitude = origin.latitude
        private val originLongitude = origin.longitude
        private val metersPerDegreeLongitude = METERS_PER_DEGREE * Trigonometry.cosDegrees(originLatitude)

        fun toMeters(coordinate: Coordinate): Vector2 {
            return toMeters(coordinate.latitude, coordinate.longitude)
        }

        fun toMeters(latitude: Double, longitude: Double): Vector2 {
            return Vector2(
                (Coordinate.toLongitude(longitude - originLongitude) * metersPerDegreeLongitude).toFloat(),
                ((latitude - originLatitude) * METERS_PER_DEGREE).toFloat()
            )
        }
    }

    private companion object {
        const val METERS_PER_DEGREE: Double = Geology.EARTH_AVERAGE_RADIUS * PI / 180.0
        const val MATCH_TOLERANCE_METERS = 1f
        const val CORNER_CHORD_METERS = 15f
        const val MIN_CORNER_CHORD_METERS = 3f
        const val CORNER_TURN_DEGREES = 35f
    }
}
