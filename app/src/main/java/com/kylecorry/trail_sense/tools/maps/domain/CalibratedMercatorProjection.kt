package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.science.geology.IGeologyService
import com.kylecorry.sol.units.Coordinate

class CalibratedMercatorProjection(
    calibration: List<Pair<PixelCoordinate, Coordinate>>,
    geology: IGeologyService = GeologyService()
) : IProjection {

    private val left = getLeft(calibration)
    private val right = getRight(calibration)
    private val top = getTop(calibration)
    private val bottom = getBottom(calibration)
    private val width = (right?.first?.x ?: 0f) - (left?.first?.x ?: 0f)
    private val height = (bottom?.first?.y ?: 0f) - (top?.first?.y ?: 0f)
    private val bounds = CoordinateBounds(
        top?.second?.latitude ?: 0.0,
        right?.second?.longitude ?: 0.0,
        bottom?.second?.latitude ?: 0.0,
        left?.second?.longitude ?: 0.0
    )

    private val projection = MercatorProjection(bounds, width to height, geology)


    override fun toCoordinate(pixel: PixelCoordinate): Coordinate? {

        if (left == null || top == null) {
            return null
        }

        val x = pixel.x - left.first.x
        val y = pixel.y - top.first.y

        return projection.toCoordinate(PixelCoordinate(x, y))
    }

    override fun toPixels(coordinate: Coordinate): PixelCoordinate? {

        if (left == null || top == null) {
            return null
        }

        val coords = projection.toPixels(coordinate)

        val x = coords.x + left.first.x
        val y = coords.y + top.first.y

        return PixelCoordinate(x, y)
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

}