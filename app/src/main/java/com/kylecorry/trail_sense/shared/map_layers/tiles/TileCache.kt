package com.kylecorry.trail_sense.shared.map_layers.tiles

import androidx.collection.LruCache
import com.kylecorry.andromeda.core.tryOrDefault

class TileCache(val source: String, sizeMegabytes: Int) :
    LruCache<String, ImageTile>(sizeMegabytes * 1024 * 1024) {

    override fun entryRemoved(
        evicted: Boolean,
        key: String,
        oldValue: ImageTile,
        newValue: ImageTile?
    ) {
        super.entryRemoved(evicted, key, oldValue, newValue)
        oldValue.image?.recycle()
        oldValue.image = null
    }

    operator fun get(tile: Tile): ImageTile? {
        return get(getKey(tile))
    }

    fun getOrPut(key: String, provider: () -> ImageTile): ImageTile {
        val current = get(key)
        if (current != null) {
            return current
        }
        val newValue = provider()
        put(key, newValue)
        return newValue
    }

    override fun sizeOf(key: String, value: ImageTile): Int {
        return tryOrDefault(0) {
            value.image?.allocationByteCount ?: 0
        }
    }

    private fun getKey(tile: Tile): String {
        return "${source}_${tile.x}_${tile.y}_${tile.z}"
    }

}