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

    val tileCache = TileCache()

    fun clearCache() {
        tileCache.clear()
    }

    suspend fun loadTiles(
        sourceSelector: TileSource,
        tiles: List<Tile>
    ) = onDefault {
        val tilesSet = tiles.toSet()
        val tilesToLoad = tiles.filter { !tileCache.contains(it) }

        var hasChanges = false

        sourceSelector.load(tilesToLoad) { tile, image ->
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
                tileCache.remove(tile)
            } else {
                tileCache.put(tile, resized)
            }
            hasChanges = true
        }

        hasChanges = hasChanges || tileCache.removeOtherThan(tilesSet)

        if (hasChanges) {
            val memoryUsage = tileCache.getMemoryAllocation()
            Log.d("TileLoader", "Tile memory usage: ${memoryUsage / 1024} KB (${tiles.size} tiles)")
        }
    }
}