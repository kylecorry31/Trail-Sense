package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath.map
import com.kylecorry.sol.math.SolMath.norm
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate

class CalibratedProjection(
    calibration: List<Pair<PixelCoordinate, Coordinate>>,
    private val projection: IMapProjection
) : IMapProjection {

    private val left = getLeft(calibration)
    private val right = getRight(calibration)
    private val top = getTop(calibration)
    private val bottom = getBottom(calibration)
    private val width = (right?.first?.x ?: 0f) - (left?.first?.x ?: 0f)
    private val height = (bottom?.first?.y ?: 0f) - (top?.first?.y ?: 0f)
    private val bounds = getBounds(calibration.map { it.second })

    private val bottomLeft = projection.toPixels(bounds.southWest)
    private val topRight = projection.toPixels(bounds.northEast)


    override fun toCoordinate(pixel: Vector2): Coordinate {
        if (left == null || top == null) {
            return Coordinate.zero
        }

        val x = map((pixel.x - left.first.x) / width, 0f, 1f, bottomLeft.x, topRight.x)
        val y = map((pixel.y - top.first.y - height) / -height, 0f, 1f, bottomLeft.y, topRight.y)

        return projection.toCoordinate(Vector2(x, y))
    }

    override fun toPixels(coordinate: Coordinate): Vector2 {

        if (left == null || top == null || bottom == null) {
            return Vector2(0f, 0f)
        }

        val coords = if (coordinate.longitude < 0 && bounds.west > 0) {
            projection.toPixels(coordinate.copy(longitude = coordinate.longitude + 360))
        } else {
            projection.toPixels(coordinate)
        }

        val x = left.first.x + width * norm(coords.x, bottomLeft.x, topRight.x)
        val y = top.first.y + height - height * norm(coords.y, bottomLeft.y, topRight.y)
        return Vector2(x, y)
    }

    private fun getLeft(pixels: List<Pair<PixelCoordinate, Coordinate>>): Pair<PixelCoordinate, Coordinate>? {
        return pixels.minByOrNull { it.first.x }
    }

    private fun getRight(pixels: List<Pair<PixelCoordinate, Coordinate>>): Pair<PixelCoordinate, Coordinate>? {
        return pixels.maxByOrNull { it.first.x }
    }

    private fun getTop(pixels: List<Pair<PixelCoordinate, Coordinate>>): Pair<PixelCoordinate, Coordinate>? {
        return pixels.minByOrNull { it.first.y }
    }

    private fun getBottom(pixels: List<Pair<PixelCoordinate, Coordinate>>): Pair<PixelCoordinate, Coordinate>? {
        return pixels.maxByOrNull { it.first.y }
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

}