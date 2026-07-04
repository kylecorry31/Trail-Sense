package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write

class ImageTile(
    val key: String,
    val tile: Tile,
    @Volatile private var image: Bitmap? = null,
    @Volatile var state: TileState = TileState.Idle,
    private val shouldFadeIn: Boolean = true,
    loadFunction: (suspend () -> Bitmap?)?
) {

    private val lock = ReentrantReadWriteLock()

    @Volatile
    private var _loadFunction: (suspend () -> Bitmap?)? = loadFunction

    @Volatile
    var loadingStartTime: Long? = null
    fun getAlpha(): Int {
        if (!shouldFadeIn) {
            return 255
        }
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
        return lock.read {
            image != null
        }
    }

    fun setLoader(loader: (suspend () -> Bitmap?)?) {
        lock.write {
            _loadFunction = loader
        }
    }

    fun invalidate() {
        lock.write {
            state = TileState.Stale
            _loadFunction = null
        }
    }

    suspend fun load() {
        var wasIdle = false
        val loader = lock.write {
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
            lock.write {
                image?.recycle()
                image = newImage
                hasImage = image != null
            }
            wasSuccess = true
        } catch (e: Throwable) {
            e.printStackTrace()
            lock.write {
                image?.recycle()
                image = null
            }
        }
        if (wasIdle) {
            loadingStartTime = System.currentTimeMillis()
        }

        lock.write {
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
        lock.read {
            block(image)
        }
    }

    fun recycle() {
        lock.write {
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
