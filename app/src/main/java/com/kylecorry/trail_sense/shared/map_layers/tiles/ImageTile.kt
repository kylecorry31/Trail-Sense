package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap

class ImageTile(
    val key: String,
    val tile: Tile,
    var image: Bitmap? = null,
    var state: TileState = TileState.Idle,
    private val loadFunction: suspend () -> Bitmap?
) {
    var loadingStartTime: Long? = null

    fun getAlpha(): Int {
        return loadingStartTime?.let { startTime ->
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed >= 250) {
                255
            } else {
                val t = elapsed / 250f
                (t * t * t * 255).toInt().coerceIn(0, 255)
            }
        } ?: 255
    }

    fun isFadingIn(): Boolean {
        return getAlpha() != 255
    }

    suspend fun load() {
        state = TileState.Loading
        loadingStartTime = System.currentTimeMillis()
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