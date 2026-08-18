package com.kylecorry.trail_sense.shared.map_layers.tiles

import androidx.collection.LruCache
import java.util.concurrent.ConcurrentHashMap

class TileCache(
    val source: String,
    maxSize: Int,
    private val removalListener: (ImageTile) -> Unit = { it.recycle() }
) : LruCache<String, ImageTile>(maxSize) {

    private val entries = ConcurrentHashMap<String, ImageTile>()
    private val transferring = ConcurrentHashMap<String, ImageTile>()

    override fun entryRemoved(
        evicted: Boolean,
        key: String,
        oldValue: ImageTile,
        newValue: ImageTile?
    ) {
        super.entryRemoved(evicted, key, oldValue, newValue)
        entries.remove(key, oldValue)
        if (!transferring.remove(key, oldValue)) {
            removalListener(oldValue)
        }
    }

    operator fun get(tile: Tile): ImageTile? {
        return get(getKey(tile))
    }

    fun peek(key: String): ImageTile? {
        return entries[key]
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

    fun store(key: String, tile: ImageTile) {
        synchronized(this) {
            put(key, tile)
            entries[key] = tile
        }
    }

    fun evictOwner(owner: String) {
        synchronized(this) {
            val keys = snapshot().filterValues { it.owner == owner }.keys
            keys.forEach { remove(it) }
        }
    }

    fun invalidateOwner(owner: String) {
        snapshot().values.filter { it.owner == owner }.forEach { it.invalidate() }
    }

    fun sizeBytes(): Long {
        var bytes = 0L
        snapshot().values.forEach { tile ->
            tile.withImage { image ->
                bytes += image?.allocationByteCount?.toLong() ?: 0L
            }
        }
        return bytes
    }

    fun take(key: String): ImageTile? {
        synchronized(this) {
            val value = get(key) ?: return null
            transferring[key] = value
            remove(key)
            return value
        }
    }

    fun takeAll(): List<ImageTile> {
        synchronized(this) {
            return snapshot().keys.mapNotNull {
                val value = get(it) ?: return@mapNotNull null
                transferring[it] = value
                remove(it)
                value
            }
        }
    }

    fun removeIfSame(key: String, tile: ImageTile): Boolean {
        synchronized(this) {
            if (entries[key] !== tile) {
                return false
            }
            remove(key)
            return true
        }
    }

    fun getKey(tile: Tile): String {
        return "${source}_${tile.x}_${tile.y}_${tile.z}"
    }

}
