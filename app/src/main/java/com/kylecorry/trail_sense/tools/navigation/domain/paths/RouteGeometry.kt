package com.kylecorry.trail_sense.tools.navigation.domain.paths

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
 * A location snapped onto the route.
 * @param segment the index of the route point that starts the segment containing the snap
 * @param percentAlongSegment how far along the segment the snap is, from 0 to 1
 * @param distanceAlong the distance along the route to the snap
 * @param offset the distance from the queried location to the snap
 */
internal class Snap(
    val segment: Int,
    val percentAlongSegment: Float,
    val distanceAlong: Float,
    val offset: Float
)

/**
 * Spatial queries over a polyline, with positions given as distances along it.
 */
internal class RouteGeometry(private val routePoints: List<Coordinate>) {
    private val latitudes = DoubleArray(routePoints.size) { routePoints[it].latitude }
    private val longitudes = DoubleArray(routePoints.size) { routePoints[it].longitude }
    private val cumulativeDistances = HikingService.getDistances(routePoints).toFloatArray()
    private val lastSegment = routePoints.lastIndex - 1
    val length: Float = cumulativeDistances.last()

    /** The distance along the route to the point at [pointIndex]. */
    fun distanceAt(pointIndex: Int): Float = cumulativeDistances[pointIndex]

    /**
     * The snaps that are within the GPS noise of the closest one, in route order. Overlapping
     * parts of a route (ex. the start and end of an out and back) are indistinguishable within the
     * GPS error, so the closest is not necessarily the right one. The route must have a length.
     */
    fun candidateSnaps(location: Coordinate): List<Snap> {
        val plane = LocalPlane(location)
        val snaps = (0..lastSegment).mapNotNull { projectOnto(it, plane, 0f, length) }
        val closest = snaps.minOf { it.offset }
        return snaps.filter { it.offset <= closest + ROUTING_SAME_PLACE_TOLERANCE_METERS }
    }

    fun coordinateOf(snap: Snap): Coordinate {
        return interpolate(snap.segment, snap.percentAlongSegment)
    }

    /**
     * Finds the closest point on the route to the location, only considering the part of the route
     * between [minDistance] and [maxDistance]. Each candidate costs its offset plus a small penalty
     * for its distance along the route from [estimatedDistance] (capped, so a far away candidate
     * that is clearly closer still wins), so the estimate only decides between candidates that are
     * about equally close. The lowest cost is used (the earliest if
     * tied). If there is no segment in that range, the start of the range is returned.
     */
    fun getOptimalLocationMatch(
        location: Coordinate,
        minDistance: Float,
        maxDistance: Float,
        estimatedDistance: Float
    ): Snap {
        val plane = LocalPlane(location)
        val snaps = (segmentContaining(minDistance)..segmentContaining(maxDistance))
            .mapNotNull { projectOnto(it, plane, minDistance, maxDistance) }
        if (snaps.isEmpty()) return snapAt(location, minDistance)

        return snaps.minBy {
            it.offset + min(
                ESTIMATE_DISTANCE_WEIGHT * abs(it.distanceAlong - estimatedDistance),
                MAX_ESTIMATE_DISTANCE_PENALTY_METERS
            )
        }
    }

    fun pointAt(distance: Float): Coordinate {
        val segment = segmentContaining(distance)
        return interpolate(segment, fractionAlong(segment, distance))
    }

    // Projects the plane's origin onto the part of the segment between minDistance and maxDistance
    private fun projectOnto(
        segment: Int,
        plane: LocalPlane,
        minDistance: Float,
        maxDistance: Float
    ): Snap? {
        val overlapsRange = cumulativeDistances[segment + 1] >= minDistance &&
                cumulativeDistances[segment] <= maxDistance
        if (segmentLength(segment) <= 0f || !overlapsRange) return null
        val a = plane.toMeters(latitudes[segment], longitudes[segment])
        val b = plane.toMeters(latitudes[segment + 1], longitudes[segment + 1])
        val fraction = closestFractionToOrigin(a, b)
            .coerceIn(fractionAlong(segment, minDistance), fractionAlong(segment, maxDistance))
        val projected = a + (b - a) * fraction
        return Snap(
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

    private fun snapAt(location: Coordinate, distance: Float): Snap {
        val clamped = distance.coerceIn(0f, length)
        val segment = segmentContaining(clamped)
        val fraction = fractionAlong(segment, clamped)
        return Snap(segment, fraction, clamped, location.distanceTo(interpolate(segment, fraction)))
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
        const val ESTIMATE_DISTANCE_WEIGHT = 0.01f
        const val MAX_ESTIMATE_DISTANCE_PENALTY_METERS = 5f
    }
}
