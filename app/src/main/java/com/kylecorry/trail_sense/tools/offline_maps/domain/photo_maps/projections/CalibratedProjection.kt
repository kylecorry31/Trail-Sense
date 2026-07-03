package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate

class CalibratedProjection(
    calibration: List<Pair<PixelCoordinate, Coordinate>>,
    private val projection: IMapProjection
) : IMapProjection {

    private val bounds = getBounds(calibration.map { it.second })
    private val transform = getTransform(calibration)

    override fun toCoordinate(pixel: Vector2): Coordinate {
        return projection.toCoordinate(transform?.toSource(pixel) ?: return Coordinate.zero)
    }

    override fun toPixels(location: Coordinate): Vector2 {
        return toPixels(location.latitude, location.longitude)
    }

    override fun toPixels(
        latitude: Double,
        longitude: Double
    ): Vector2 {
        val coords = if (longitude < 0 && bounds.west > 0) {
            projection.toPixels(latitude, longitude + 360)
        } else {
            projection.toPixels(latitude, longitude)
        }

        return transform?.toDestination(coords) ?: Vector2(0f, 0f)
    }

    private fun getBounds(coordinates: List<Coordinate>): CoordinateBounds {
        var bounds = CoordinateBounds.from(coordinates)

        val minLongitude = coordinates.minByOrNull { it.longitude }?.longitude
        val maxLongitude = coordinates.maxByOrNull { it.longitude }?.longitude

        // TODO: Move this into the coordinate bounds class
        if (minLongitude == -180.0 && maxLongitude == 180.0) {
            bounds = CoordinateBounds(bounds.north, 180.0, bounds.south, -180.0)
        }

        // TODO: Map projection does not work at the poles - move this into the mercator projection class
        if (bounds.north == 90.0) {
            bounds = CoordinateBounds(89.9999, bounds.east, bounds.south, bounds.west)
        }

        if (bounds.south == -90.0) {
            bounds = CoordinateBounds(bounds.north, bounds.east, -89.9999, bounds.west)
        }

        // TODO: This should be moved into the mercator projection class
        if (bounds.east < 0 && bounds.west > 0) {
            bounds =
                CoordinateBounds(bounds.north, bounds.east + 360, bounds.south, bounds.west)
        }

        return bounds
    }

    private fun getTransform(calibration: List<Pair<PixelCoordinate, Coordinate>>): Transform? {
        if (calibration.size < 2) {
            return null
        }

        val points = calibration.map {
            toProjectionPixels(it.second.latitude, it.second.longitude) to Vector2(
                it.first.x,
                it.first.y
            )
        }

        return getSimilarityTransform(points[0], points[1])
    }

    private fun getSimilarityTransform(
        first: Pair<Vector2, Vector2>,
        second: Pair<Vector2, Vector2>
    ): Transform? {
        val sourceDx = second.first.x - first.first.x
        val sourceDy = second.first.y - first.first.y
        val destinationDx = second.second.x - first.second.x
        val destinationDy = second.second.y - first.second.y
        val sourceLengthSquared = sourceDx * sourceDx + sourceDy * sourceDy
        if (sourceLengthSquared == 0f) {
            return null
        }

        val a = (destinationDx * sourceDx - destinationDy * sourceDy) / sourceLengthSquared
        val b = (destinationDx * sourceDy + destinationDy * sourceDx) / sourceLengthSquared
        val c = first.second.x - a * first.first.x - b * first.first.y
        val d = (destinationDy * sourceDx + destinationDx * sourceDy) / sourceLengthSquared
        val e = (destinationDy * sourceDy - destinationDx * sourceDx) / sourceLengthSquared
        val f = first.second.y - d * first.first.x - e * first.first.y

        return Transform(a, b, c, d, e, f)
    }

    private data class Transform(
        val a: Float,
        val b: Float,
        val c: Float,
        val d: Float,
        val e: Float,
        val f: Float
    ) {
        fun toDestination(source: Vector2): Vector2 {
            return Vector2(
                a * source.x + b * source.y + c,
                d * source.x + e * source.y + f
            )
        }

        fun toSource(destination: Vector2): Vector2? {
            val determinant = a * e - b * d
            if (determinant == 0f) {
                return null
            }

            val x = destination.x - c
            val y = destination.y - f
            return Vector2(
                (e * x - b * y) / determinant,
                (-d * x + a * y) / determinant
            )
        }
    }

    private fun toProjectionPixels(latitude: Double, longitude: Double): Vector2 {
        return if (longitude < 0 && bounds.west > 0) {
            projection.toPixels(latitude, longitude + 360)
        } else {
            projection.toPixels(latitude, longitude)
        }
    }

}
