package com.kylecorry.trail_sense.tools.maps.infrastructure.tiles

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geology
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sinh
import kotlin.math.tan

object TileMath {

    fun getTiles(
        bounds: CoordinateBounds,
        widthPx: Int,
        heightPx: Int
    ): List<Tile> {
        val minLat = max(bounds.south, -85.0511)
        val maxLat = min(bounds.north, 85.0511)
        return getTiles(
            bounds,
            boundsToZoom(minLat, bounds.west, maxLat, bounds.east, widthPx, heightPx)
        )
    }

    fun getTiles(
        bounds: CoordinateBounds,
        metersPerPixel: Double
    ): List<Tile> {
        val minLat = max(bounds.south, -85.0511)
        val maxLat = min(bounds.north, 85.0511)
        return getTiles(
            bounds,
            distancePerPixelToZoom(metersPerPixel, (minLat + maxLat) / 2)
        )
    }

    fun getTiles(
        bounds: CoordinateBounds,
        zoom: Int
    ): List<Tile> {
        val minLat = max(bounds.south, -85.0511)
        val maxLat = min(bounds.north, 85.0511)

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

    private fun boundsToZoom(
        minLat: Double,
        minLon: Double,
        maxLat: Double,
        maxLon: Double,
        widthPx: Int,
        heightPx: Int
    ): Int {
        fun mercatorY(lat: Double): Double {
            val rad = Math.toRadians(lat)
            return ln(tan(PI / 4 + rad / 2))
        }

        val worldTileSize = 256.0
        val latFraction = (mercatorY(maxLat) - mercatorY(minLat)) / (2 * PI)
        val lonFraction = (maxLon - minLon) / 360.0

        val latZoom = log2(heightPx / worldTileSize / latFraction)
        val lonZoom = log2(widthPx / worldTileSize / lonFraction)

        return floor(min(latZoom, lonZoom)).toInt()
    }

    private fun distancePerPixelToZoom(
        distancePerPixel: Double,
        latitude: Double
    ): Int {
        val eC = Geology.EARTH_AVERAGE_RADIUS * 2 * PI
        val earthCircumference = 40075017.0 // in meters
        val metersPerPixel =
            earthCircumference * cos(Math.toRadians(latitude)) / (256 * (1 shl 0)) // 256 pixels at zoom level 0
        return log2(metersPerPixel / distancePerPixel).toInt()
    }

}