package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource

class ImageTile(
    val key: String,
    val tile: Tile,
    var image: Bitmap? = null,
    var state: TileState = TileState.Idle,
    private val loadFunction: suspend () -> Bitmap?
) {
    suspend fun load() {
        state = TileState.Loading
        val wasSuccess = try {
            image = loadFunction()
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            image = null
            false
        }

        state = when {
            wasSuccess && image != null -> TileState.Loaded
            wasSuccess -> TileState.Empty
            else -> TileState.Error
        }

    }
}

enum class TileState {
    Idle,
    Loading,
    Loaded,
    Error,
    Empty
}