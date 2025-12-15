package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap

interface TileSource {
    suspend fun load(tiles: List<Tile>, onLoaded: suspend (Tile, Bitmap?) -> Unit)
}