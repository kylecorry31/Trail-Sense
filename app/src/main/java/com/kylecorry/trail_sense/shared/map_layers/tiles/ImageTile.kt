package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap

class ImageTile(
    val key: String,
    val tile: Tile,
    private var image: Bitmap? = null,
    var state: TileState = TileState.Idle,
    loadFunction: (suspend () -> Bitmap?)?
) {

    private val lock = Any()

    private var _loadFunction: (suspend () -> Bitmap?)? = loadFunction

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

    fun hasImage(): Boolean {
        synchronized(lock) {
            return image != null
        }
    }

    fun setLoader(loader: (suspend () -> Bitmap?)?) {
        synchronized(lock) {
            _loadFunction = loader
        }
    }

    fun invalidate() {
        synchronized(lock) {
            state = TileState.Stale
            _loadFunction = null
        }
    }

    suspend fun load() {
        var wasIdle = false
        val loader = synchronized(lock) {
            if (_loadFunction == null) {
                state = TileState.Stale
                return
            }
            wasIdle = state == TileState.Idle
            state = TileState.Loading
            _loadFunction
        }

        var hasImage = false
        var wasSuccess = false
        try {
            val newImage = loader?.invoke()
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
        if (wasIdle) {
            loadingStartTime = System.currentTimeMillis()
        }

        synchronized(lock) {
            if (state == TileState.Stale) {
                return
            }
            state = when {
                wasSuccess && hasImage -> TileState.Loaded
                wasSuccess -> TileState.Empty
                else -> TileState.Error
            }
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
    Empty,
    Stale
}