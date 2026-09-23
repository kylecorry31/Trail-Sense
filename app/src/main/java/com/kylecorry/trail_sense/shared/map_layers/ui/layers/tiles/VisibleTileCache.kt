package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath

/** Keeps only the most recent viewport; tile image availability is deliberately not cached. */
internal class VisibleTileCache(private val maxTiles: Int) {
    private var lastBounds: CoordinateBounds? = null
    private var lastZoom = 0
    private var lastOffset = 0
    private var tiles = emptyList<Tile>()

    fun get(bounds: CoordinateBounds, zoom: Int, offset: Int): List<Tile> {
        if (bounds == lastBounds && zoom == lastZoom && offset == lastOffset) {
            return tiles
        }
        var adjustedOffset = offset + 1
        do {
            adjustedOffset--
            tiles = TileMath.getTiles(bounds, (zoom + adjustedOffset).coerceAtMost(20))
        } while (tiles.size > maxTiles && (zoom + adjustedOffset) > 1)
        lastBounds = bounds
        lastZoom = zoom
        lastOffset = offset
        return tiles
    }
}
