package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import android.graphics.Bitmap
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile

interface TileSource {
    suspend fun load(tiles: List<Tile>, onLoaded: suspend (Tile, Bitmap?) -> Unit)
}