package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Coordinate
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
        metersPerPixel: Double,
        maxZoom: Int = 20
    ): List<Tile> {
        val minLat = max(bounds.south, MIN_LATITUDE)
        val maxLat = min(bounds.north, MAX_LATITUDE)
        return getTiles(
            bounds,
            distancePerPixelToZoom(metersPerPixel, (minLat + maxLat) / 2).coerceAtMost(maxZoom)
        )
    }

    fun getTiles(
        bounds: CoordinateBounds,
        zoom: Int
    ): List<Tile> {
        // If the bounds crosses the -180/180 line, split this into 2 calls - one for each hemisphere
        if (Coordinate.toLongitude(bounds.west) > Coordinate.toLongitude(bounds.east)) {
            val leftBounds = CoordinateBounds(
                bounds.south,
                180.0,
                bounds.north,
                Coordinate.toLongitude(bounds.west)
            )
            val rightBounds = CoordinateBounds(
                bounds.south,
                Coordinate.toLongitude(bounds.east),
                bounds.north,
                -180.0
            )
            return getTiles(leftBounds, zoom) + getTiles(rightBounds, zoom)
        }

        val minLat = max(bounds.south, MIN_LATITUDE)
        val maxLat = min(bounds.north, MAX_LATITUDE)

        val (xMin, yMax) = latLonToTileXY(minLat, bounds.west, zoom)
        val (xMax, yMin) = latLonToTileXY(maxLat, bounds.east, zoom)

        // If range is greater than 100, return an empty list
        if (xMax - xMin > 100 || yMax - yMin > 100) {
            return emptyList()
        }

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

    fun distancePerPixelToZoom(
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