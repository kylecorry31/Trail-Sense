package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import android.graphics.Bitmap
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile

interface TileSource {
    suspend fun loadTile(tile: Tile): Bitmap?
    suspend fun cleanup(){
        // Do nothing
    }
}