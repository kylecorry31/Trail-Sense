package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class ImageTile(
    val key: String,
    val tile: Tile,
    @Volatile private var image: Bitmap? = null,
    state: TileState = TileState.Idle,
    private val shouldFadeIn: Boolean = true,
    loadFunction: (suspend () -> Bitmap?)?
) {

    private val imageLock = ReentrantReadWriteLock()
    private val stateLock = Any()

    @Volatile
    private var _loadFunction: (suspend () -> Bitmap?)? = loadFunction

    @Volatile
    var state: TileState = state
        private set

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
        return imageLock.read { image != null }
    }

    fun setLoader(loader: (suspend () -> Bitmap?)) {
        _loadFunction = loader
    }

    fun invalidate() {
        _loadFunction = null
        synchronized(stateLock) {
            state = TileState.Stale
        }
    }

    suspend fun load() {
        var wasIdle = false
        val loader = synchronized(stateLock) {
            val loadFn = _loadFunction
            if (loadFn == null) {
                state = TileState.Stale
                return
            }
            wasIdle = state == TileState.Idle
            state = TileState.Loading
            loadFn
        }

        var hasImage = false
        var wasSuccess = false
        try {
            val newImage = loader.invoke()
            imageLock.write {
                image?.recycle()
                image = newImage
                hasImage = image != null
            }
            wasSuccess = true
        } catch (e: Throwable) {
            e.printStackTrace()
            imageLock.write {
                image?.recycle()
                image = null
            }
        }
        if (wasIdle) {
            loadingStartTime = System.currentTimeMillis()
        }

        synchronized(stateLock) {
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
        imageLock.read {
            block(image)
        }
    }

    fun recycle() {
        imageLock.write {
            image?.recycle()
            image = null
            synchronized(stateLock) {
                state = TileState.Idle
            }
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
