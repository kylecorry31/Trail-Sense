package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class TileCache {

    private var readable = mapOf<Tile, Bitmap>()

    private val writable = ConcurrentHashMap<Tile, Bitmap>()

    private val toRecycle = ConcurrentLinkedQueue<Bitmap>()
    private val lock = Any()

    fun clear() {
        synchronized(lock) {
            while (toRecycle.isNotEmpty()) {
                toRecycle.poll()?.recycle()
            }
            readable.values.forEach { it.recycle() }
            writable.values.forEach { it.recycle() }
            readable = mapOf()
            writable.clear()
        }
    }

    fun withRead(block: (cache: Map<Tile, Bitmap>) -> Unit) {
        synchronized(lock) {
            while (toRecycle.isNotEmpty()) {
                toRecycle.poll()?.recycle()
            }
            readable = writable.toMap()
            block(readable)
        }
    }

    fun put(tile: Tile, bitmap: Bitmap) {
        val old = writable.put(tile, bitmap)
        old?.let { toRecycle.add(it) }
    }

    fun get(tile: Tile): Bitmap? {
        return writable[tile]
    }

    fun remove(tile: Tile) {
        val old = writable.remove(tile)
        old?.let { toRecycle.add(it) }
    }

    fun contains(tile: Tile): Boolean {
        return writable.contains(tile)
    }

    fun keys(): Set<Tile> {
        return writable.keys
    }

    /**
     * Removes tiles other than the ones in the set
     * @param tilesToKeep The tiles to keep
     * @param zFilter If not null, only tiles with a z value matching the filter will be removed
     */
    fun removeOtherThan(tilesToKeep: Set<Tile>, zFilter: Int? = null): Boolean {
        val keysToRemove =
            writable.keys.filter { it !in tilesToKeep && (zFilter == null || it.z == zFilter) }
        keysToRemove.forEach { key ->
            val old = writable.remove(key)
            old?.let { toRecycle.add(it) }
        }
        return keysToRemove.isNotEmpty()
    }

    fun getMemoryAllocation(): Int {
        return writable.values.sumOf { it.allocationByteCount }
    }

}