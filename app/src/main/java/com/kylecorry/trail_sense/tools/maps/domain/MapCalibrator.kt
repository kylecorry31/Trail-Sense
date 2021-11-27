package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.units.Coordinate

class MapCalibrator {

    private val geology = GeologyService()

    fun getCoordinate(
        pixel: PixelCoordinate,
        calibration: List<Pair<PixelCoordinate, Coordinate>>
    ): Coordinate? {
        val left = getLeft(calibration) ?: return null
        val right = getRight(calibration) ?: return null
        val top = getTop(calibration) ?: return null
        val bottom = getBottom(calibration) ?: return null

        val width = right.first.x - left.first.x
        val height = bottom.first.y - top.first.y

        val bounds = CoordinateBounds(
            top.second.latitude,
            right.second.longitude,
            bottom.second.latitude,
            left.second.longitude
        )

        val x = pixel.x - left.first.x
        val y = pixel.y - top.first.y

        return geology.fromMercator(Vector2(x, y), bounds, width to height)
    }

    fun getPixel(
        coordinate: Coordinate,
        calibration: List<Pair<PixelCoordinate, Coordinate>>
    ): PixelCoordinate? {
        val left = getLeft(calibration) ?: return null
        val right = getRight(calibration) ?: return null
        val top = getTop(calibration) ?: return null
        val bottom = getBottom(calibration) ?: return null

        val width = right.first.x - left.first.x
        val height = bottom.first.y - top.first.y

        val bounds = CoordinateBounds(
            top.second.latitude,
            right.second.longitude,
            bottom.second.latitude,
            left.second.longitude
        )

        val coords = geology.toMercator(coordinate, bounds, width to height)

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