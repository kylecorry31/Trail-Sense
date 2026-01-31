package com.kylecorry.trail_sense.shared.map_layers.tiles

import com.kylecorry.sol.science.geology.CoordinateBounds
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

        val southWest = getTile(minLat, bounds.west, zoom)
        val northEast = getTile(maxLat, bounds.east, zoom)

        val n = 1 shl zoom
        val xMin = southWest.x.coerceAtMost(n - 1)
        val xMax = northEast.x.coerceAtMost(n - 1)
        val yMin = northEast.y.coerceAtMost(n - 1)
        val yMax = southWest.y.coerceAtMost(n - 1)

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

    fun snapToTiles(
        bounds: CoordinateBounds,
        zoom: Int,
        maxZoom: Int = 20,
        growBy: Int = 0
    ): CoordinateBounds {
        val actualZoom = zoom.coerceAtMost(maxZoom)
        val northWestTile =
            getTile(bounds.north, bounds.west, actualZoom).getNeighbor(-growBy, -growBy)
                .getBounds()
        val southEastTile =
            getTile(bounds.south, bounds.east, actualZoom).getNeighbor(growBy, growBy)
                .getBounds()
        return CoordinateBounds(
            northWestTile.north,
            southEastTile.east,
            southEastTile.south,
            northWestTile.west
        )
    }

    fun getTile(lat: Double, lon: Double, zoom: Int): Tile {
        val latRad = Math.toRadians(lat)
        val n = 1 shl zoom
        val x = ((lon + 180.0) / 360.0 * n).toInt()
        val y = ((1.0 - ln(tan(latRad) + 1 / cos(latRad)) / PI) / 2.0 * n).toInt()
        return Tile(x, y, zoom)
    }

    fun getZoomLevel(coordinate: Coordinate, resolution: Float): Float {
        val latitude = coordinate.latitude
        val earthCircumference = WEB_MERCATOR_RADIUS * 2 * PI
        val sourceResolution =
            earthCircumference * cos(Math.toRadians(latitude)) / WORLD_TILE_SIZE
        return log2(sourceResolution / resolution).toFloat()

    }

    private const val MIN_LATITUDE = -85.0511
    private const val MAX_LATITUDE = 85.0511
    const val WORLD_TILE_SIZE = 256
    const val WEB_MERCATOR_RADIUS = 6378137.0
}