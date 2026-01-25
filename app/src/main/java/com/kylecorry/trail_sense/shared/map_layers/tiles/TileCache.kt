package com.kylecorry.trail_sense.shared.map_layers.tiles

import androidx.collection.LruCache

class TileCache(val source: String, maxSize: Int) : LruCache<String, ImageTile>(maxSize) {

    override fun entryRemoved(
        evicted: Boolean,
        key: String,
        oldValue: ImageTile,
        newValue: ImageTile?
    ) {
        super.entryRemoved(evicted, key, oldValue, newValue)
        oldValue.image?.recycle()
        oldValue.image = null
        oldValue.state = TileState.Idle
    }

    operator fun get(tile: Tile): ImageTile? {
        return get(getKey(tile))
    }

    fun peek(tile: Tile): ImageTile? {
        return snapshot()[getKey(tile)]
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

    private fun getKey(tile: Tile): String {
        return "${source}_${tile.x}_${tile.y}_${tile.z}"
    }

}