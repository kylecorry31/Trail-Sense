package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap

class ImageTile(
    val key: String,
    val tile: Tile,
    private var image: Bitmap? = null,
    var state: TileState = TileState.Idle,
    private val loadFunction: suspend () -> Bitmap?
) {

    private val lock = Any()

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
        var hasImage = false
        var wasSuccess = false
        try {
            val newImage = loadFunction()
            synchronized(lock) {
                image?.recycle()
                image = newImage
                hasImage = image != null
            }
            wasSuccess = true
        } catch (e: Throwable) {
            e.printStackTrace()
            synchronized(lock) {
                image?.recycle()
                image = null
            }
        }

        state = when {
            wasSuccess && hasImage -> TileState.Loaded
            wasSuccess -> TileState.Empty
            else -> TileState.Error
        }

    }

    fun withImage(block: (image: Bitmap?) -> Unit) {
        synchronized(lock) {
            block(image)
        }
    }

    fun recycle() {
        synchronized(lock) {
            image?.recycle()
            image = null
            state = TileState.Idle
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