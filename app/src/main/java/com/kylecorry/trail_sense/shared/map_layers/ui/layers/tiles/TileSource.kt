package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.core.os.bundleOf
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile

interface TileSource {
    suspend fun loadTile(
        context: Context,
        tile: Tile,
        params: Bundle = bundleOf()
    ): Bitmap?
    suspend fun cleanup() {
        // Do nothing
    }

    companion object {
        const val PARAM_TIME = "time"
        const val PARAM_PREFERENCES = "preferences"
    }
}
