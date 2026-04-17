package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile

interface TileSource {
    suspend fun loadTile(
        context: Context,
        tile: Tile,
        params: Bundle = Bundle()
    ): Bitmap?

    suspend fun cleanup() {
        // Do nothing
    }

}
