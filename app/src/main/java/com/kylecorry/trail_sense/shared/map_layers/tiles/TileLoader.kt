package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import android.util.Log
import com.kylecorry.luna.coroutines.onDefault

class TileLoader {

    var tileCache: Map<Tile, List<Bitmap>> = emptyMap()
        private set

    var lock = Any()

    var alwaysReloadTiles: Boolean = false
    var clearTileWhenNullResponse: Boolean = true

    fun clearCache() {
        synchronized(lock) {
            tileCache.forEach { (_, bitmaps) ->
                bitmaps.forEach { it.recycle() }
            }
            tileCache = emptyMap()
        }
    }

    suspend fun loadTiles(
        sourceSelector: TileSource,
        tiles: List<Tile>
    ) = onDefault {
        val tilesToLoad = if (alwaysReloadTiles) {
            tiles
        } else {
            tiles.filter { !tileCache.containsKey(it) }
        }

        var hasChanges = false

        sourceSelector.load(tilesToLoad) { tile, image ->
            synchronized(lock) {
                if (clearTileWhenNullResponse || image != null) {
                    val old = tileCache[tile]
                    if (image == null) {
                        tileCache -= tile
                    } else {
                        tileCache += tile to listOfNotNull(image)
                    }
                    old?.forEach { it.recycle() }
                    hasChanges = true
                }
            }
        }

        synchronized(lock) {
            val tilesSet = tiles.toSet()
            val keysToRemove = tileCache.keys.filter { it !in tilesSet }
            keysToRemove.forEach { key ->
                tileCache[key]?.forEach { bitmap -> bitmap.recycle() }
                tileCache -= key
                hasChanges = true
            }
        }

        if (hasChanges) {
            System.gc()
            val memoryUsage = tileCache.values.sumOf { bitmaps ->
                bitmaps.sumOf { it.allocationByteCount }
            }
            Log.d("TileLoader", "Tile memory usage: ${memoryUsage / 1024} KB (${tiles.size} tiles)")
        }
    }
}