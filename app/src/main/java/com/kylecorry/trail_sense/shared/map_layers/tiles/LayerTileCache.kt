package com.kylecorry.trail_sense.shared.map_layers.tiles

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class LayerTileCache(
    val source: String,
    maxSize: Int
) {

    // This lock coordinates normal cache operations with whole-layer lifecycle operations. A read lock does not mean
    // the cache is immutable: it allows thread-safe LRU mutations to run concurrently while preventing clear or
    // invalidation. The write lock gives those lifecycle operations exclusive access to both cache tiers.
    private val lifecycleLock = ReentrantReadWriteLock()
    private val layerCache = TileCache(source, maxSize) { tile ->
        if (isCacheable(tile)) {
            sharedCache.store(tile.key, tile)
        } else {
            recycle(tile)
        }
    }

    operator fun get(tile: Tile): ImageTile? {
        return layerCache[tile]
    }

    fun peek(tile: Tile): ImageTile? {
        val key = getKey(tile)
        return layerCache.peek(key) ?: sharedCache.peek(key)
    }

    fun getOrPut(
        tiles: List<Tile>,
        provider: (tile: Tile, key: String) -> ImageTile
    ): List<ImageTile> {
        // Batch normal cache mutations under one shared lock acquisition for the rendering hot path.
        return lifecycleLock.read {
            tiles.map { tile ->
                getOrPutLocked(tile) { key -> provider(tile, key) }
            }
        }
    }

    fun onLoadComplete(tile: ImageTile) {
        if (!isCacheable(tile)) {
            sharedCache.removeIfSame(tile.key, tile)
        }
    }

    fun clear() {
        lifecycleLock.write {
            layerCache.takeAll().forEach { recycle(it) }
            sharedCache.evictOwner(source)
        }
    }

    fun invalidate() {
        lifecycleLock.write {
            layerCache.snapshot().values.forEach { it.invalidate() }
            sharedCache.invalidateOwner(source)
        }
    }

    fun resize(maxSize: Int) {
        // Resizing mutates the thread-safe LRU, but only needs to be excluded from whole-layer lifecycle operations.
        lifecycleLock.read {
            layerCache.resize(maxSize)
        }
    }

    fun maxSize(): Int {
        return layerCache.maxSize()
    }

    fun sizeBytes(): Long {
        return layerCache.sizeBytes()
    }

    fun sharedSizeBytes(): Long {
        return sharedCache.sizeBytes()
    }

    private fun getOrPutLocked(tile: Tile, provider: (key: String) -> ImageTile): ImageTile {
        val key = getKey(tile)
        return layerCache.getOrPut(key) {
            takeShared(key) ?: provider(key)
        }
    }

    private fun takeShared(key: String): ImageTile? {
        val tile = sharedCache.take(key) ?: return null
        if (isCacheable(tile)) {
            return tile
        }
        recycle(tile)
        return null
    }

    private fun getKey(tile: Tile): String {
        return "${source}_${tile.x}_${tile.y}_${tile.z}"
    }

    private fun isCacheable(tile: ImageTile): Boolean {
        return tile.hasImage() || tile.state == TileState.Loading || tile.state == TileState.Empty
    }

    companion object {
        private val recycleScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val sharedCache = TileCache("", SHARED_CACHE_SIZE) { recycle(it) }
        private const val SHARED_CACHE_SIZE = 256

        private fun recycle(tile: ImageTile) {
            recycleScope.launch {
                tile.recycle()
            }
        }
    }
}
