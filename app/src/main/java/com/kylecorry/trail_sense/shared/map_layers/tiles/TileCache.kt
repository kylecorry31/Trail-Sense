package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import java.util.concurrent.ConcurrentHashMap

class TileCache {

    private var readable = mapOf<Tile, Bitmap>()

    private val writable = ConcurrentHashMap<Tile, Bitmap>()
    private val lock = Any()

    fun clear() {
        synchronized(lock) {
            readable = mapOf()
            writable.clear()
        }
    }

    fun withRead(block: (cache: Map<Tile, Bitmap>) -> Unit) {
        synchronized(lock) {
            readable = writable.toMap()
            block(readable)
        }
    }

    fun put(tile: Tile, bitmap: Bitmap) {
        writable[tile] = bitmap
    }

    fun get(tile: Tile): Bitmap? {
        return writable[tile]
    }

    fun remove(tile: Tile) {
        writable.remove(tile)
    }

    fun contains(tile: Tile): Boolean {
        return writable.contains(tile)
    }

    fun removeOtherThan(tilesToKeep: Set<Tile>): Boolean {
        val keysToRemove = writable.keys.filter { it !in tilesToKeep }
        keysToRemove.forEach { key ->
            writable.remove(key)
        }
        return keysToRemove.isNotEmpty()
    }

    fun getMemoryAllocation(): Int {
        return writable.values.sumOf { it.allocationByteCount }
    }

}