package com.kylecorry.trail_sense.tools.maps.infrastructure.tiles

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geology
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

object TileMath {

    fun getTiles(
        bounds: CoordinateBounds,
        metersPerPixel: Double
    ): List<Tile> {
        val minLat = max(bounds.south, MIN_LATITUDE)
        val maxLat = min(bounds.north, MAX_LATITUDE)
        return getTiles(
            bounds,
            distancePerPixelToZoom(metersPerPixel, (minLat + maxLat) / 2)
        )
    }

    fun getTiles(
        bounds: CoordinateBounds,
        zoom: Int
    ): List<Tile> {
        val minLat = max(bounds.south, MIN_LATITUDE)
        val maxLat = min(bounds.north, MAX_LATITUDE)

        val (xMin, yMax) = latLonToTileXY(minLat, bounds.west, zoom)
        val (xMax, yMin) = latLonToTileXY(maxLat, bounds.east, zoom)

        val tiles = mutableListOf<Tile>()
        for (x in min(xMin, xMax)..max(xMin, xMax)) {
            for (y in min(yMin, yMax)..max(yMin, yMax)) {
                tiles.add(Tile(x, y, zoom))
            }
        }

        return tiles
    }

    private fun latLonToTileXY(lat: Double, lon: Double, zoom: Int): Pair<Int, Int> {
        val latRad = Math.toRadians(lat)
        val n = 1 shl zoom
        val x = ((lon + 180.0) / 360.0 * n).toInt()
        val y = ((1.0 - ln(tan(latRad) + 1 / cos(latRad)) / PI) / 2.0 * n).toInt()
        return x to y
    }

    private fun distancePerPixelToZoom(
        distancePerPixel: Double,
        latitude: Double
    ): Int {
        val earthCircumference = Geology.EARTH_AVERAGE_RADIUS * 2 * PI
        val metersPerPixel =
            earthCircumference * cos(Math.toRadians(latitude)) / (WORLD_TILE_SIZE * (1 shl 0))
        return log2(metersPerPixel / distancePerPixel).toInt()
    }

    private const val MIN_LATITUDE = -85.0511
    private const val MAX_LATITUDE = 85.0511
    const val WORLD_TILE_SIZE = 256

}