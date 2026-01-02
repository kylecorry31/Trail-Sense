package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.kylecorry.andromeda.bitmaps.operations.Resize
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.trail_sense.shared.andromeda_temp.Pad
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource

class TileLoader(private val padding: Int = 0) {

    var tileCache: Map<Tile, Bitmap> = emptyMap()
        private set

    var lock = Any()

    var alwaysReloadTiles: Boolean = false
    var clearTileWhenNullResponse: Boolean = true

    fun clearCache() {
        synchronized(lock) {
            tileCache.forEach { (_, bitmap) ->
                bitmap.recycle()
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
                    val resized = image?.applyOperationsOrNull(
                        Resize(
                            tile.size,
                            exact = false
                        ),
                        Pad(
                            padding,
                            if (image.config == Bitmap.Config.ARGB_8888) Color.TRANSPARENT else Color.WHITE
                        )
                    )
                    if (resized == null) {
                        tileCache -= tile
                    } else {
                        tileCache += tile to resized
                    }
                    old?.recycle()
                    hasChanges = true
                }
            }
        }

        synchronized(lock) {
            val tilesSet = tiles.toSet()
            val keysToRemove = tileCache.keys.filter { it !in tilesSet }
            keysToRemove.forEach { key ->
                tileCache[key]?.recycle()
                tileCache -= key
                hasChanges = true
            }
        }

        if (hasChanges) {
            System.gc()
            val memoryUsage = tileCache.values.sumOf { it.allocationByteCount }
            Log.d("TileLoader", "Tile memory usage: ${memoryUsage / 1024} KB (${tiles.size} tiles)")
        }
    }
}