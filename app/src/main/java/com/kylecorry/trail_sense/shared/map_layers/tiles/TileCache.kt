package com.kylecorry.trail_sense.shared.map_layers.tiles

import androidx.collection.LruCache
import java.util.concurrent.ConcurrentHashMap

class TileCache(val source: String, maxSize: Int) : LruCache<String, ImageTile>(maxSize) {

    private val entries = ConcurrentHashMap<String, ImageTile>()

    override fun entryRemoved(
        evicted: Boolean,
        key: String,
        oldValue: ImageTile,
        newValue: ImageTile?
    ) {
        super.entryRemoved(evicted, key, oldValue, newValue)
        entries.remove(key, oldValue)
        oldValue.recycle()
    }

    operator fun get(tile: Tile): ImageTile? {
        return get(getKey(tile))
    }

    fun peek(tile: Tile): ImageTile? {
        return entries[getKey(tile)]
    }

    fun getOrPut(key: String, provider: () -> ImageTile): ImageTile {
        synchronized(this) {
            val current = get(key)
            if (current != null) {
                return current
            }
            val newValue = provider()
            put(key, newValue)
            entries[key] = newValue
            return newValue
        }
    }

    private fun getKey(tile: Tile): String {
        return "${source}_${tile.x}_${tile.y}_${tile.z}"
    }

}
