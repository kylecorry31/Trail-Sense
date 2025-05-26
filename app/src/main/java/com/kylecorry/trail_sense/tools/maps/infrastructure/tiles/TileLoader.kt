package com.kylecorry.trail_sense.tools.maps.infrastructure.tiles

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap

class TileLoader {

    var tileCache: Map<Tile, List<Bitmap>> = emptyMap()
        private set

    var lock = Any()

    fun clearCache() {
        synchronized(lock) {
            tileCache.forEach { (_, bitmaps) ->
                bitmaps.forEach { it.recycle() }
            }
            tileCache = emptyMap()
        }
    }

    suspend fun loadTiles(maps: List<PhotoMap>, bounds: CoordinateBounds, metersPerPixel: Float) {
        // Step 1: Split the visible area into tiles (geographic)
        val tiles = TileMath.getTiles(bounds, metersPerPixel.toDouble())

        // Step 2: For each tile, determine which map(s) will supply it.
        val tileSources = mutableMapOf<Tile, List<PhotoMap>>()
        val sourceSelector = MercatorTileSourceSelector(maps)
        for (tile in tiles) {
            val sources = sourceSelector.getSources(tile.getBounds())
            if (sources.isNotEmpty()) {
                tileSources[tile] = sources.take(2)
            }
        }

        val newTiles = mutableMapOf<Tile, List<Bitmap>>()
        synchronized(lock) {
            tileCache.keys.forEach { key ->
                if (!tileSources.containsKey(key)) {
                    tileCache[key]?.forEach { bitmap -> bitmap.recycle() }
                } else {
                    // If the tile is still relevant, keep it
                    newTiles[key] = tileCache[key]!!
                }
            }

            tileCache = emptyMap()
        }
        for (source in tileSources) {
            if (newTiles.containsKey(source.key)) {
                continue
            }
            // Load tiles from the bitmap
            val entries = mutableListOf<Bitmap>()
            source.value.forEach {
                val loader = PhotoMapRegionLoader(it)
                loader.load(source.key, Size(256, 256))?.let { entries.add(it) }
            }

            newTiles[source.key] = entries
        }

        synchronized(lock) {
            tileCache = newTiles
        }
    }
}