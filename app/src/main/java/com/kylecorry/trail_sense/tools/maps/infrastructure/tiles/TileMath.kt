package com.kylecorry.trail_sense.tools.maps.infrastructure.tiles

import com.kylecorry.sol.science.geology.CoordinateBounds
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
    ): List<CoordinateBounds> {
        val minLat = max(bounds.south, -85.0511)
        val maxLat = min(bounds.north, 85.0511)
        val zoom = boundsToZoom(minLat, bounds.west, maxLat, bounds.east, widthPx, heightPx)

        val (xMin, yMax) = latLonToTileXY(minLat, bounds.west, zoom)
        val (xMax, yMin) = latLonToTileXY(maxLat, bounds.east, zoom)

        val tiles = mutableListOf<CoordinateBounds>()
        for (x in min(xMin, xMax)..max(xMin, xMax)) {
            for (y in min(yMin, yMax)..max(yMin, yMax)) {
                tiles.add(tileXYToBounds(x, y, zoom))
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

    private fun tileXYToBounds(x: Int, y: Int, zoom: Int): CoordinateBounds {
        val n = 1 shl zoom
        val lonMin = x / n.toDouble() * 360.0 - 180.0
        val lonMax = (x + 1) / n.toDouble() * 360.0 - 180.0

        val latRadMin = atan(sinh(PI * (1 - 2 * (y + 1).toDouble() / n)))
        val latRadMax = atan(sinh(PI * (1 - 2 * y.toDouble() / n)))

        val latMin = Math.toDegrees(latRadMin)
        val latMax = Math.toDegrees(latRadMax)

        return CoordinateBounds(latMin, lonMax, latMax, lonMin)
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

}