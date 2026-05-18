package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge

import androidx.collection.LruCache
import org.mapsforge.core.graphics.TileBitmap
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.queue.Job
import org.mapsforge.map.model.common.Observer

/**
 * A tile cache which only caches the fact that a job ran
 */
class MapsforgeMockTileCache(maxSize: Int) : TileCache {

    private val jobCache = LruCache<String, Boolean>(maxSize)
    private val lock = Any()

    override fun containsKey(p0: Job?): Boolean {
        return synchronized(lock) {
            jobCache[p0?.key ?: ""] ?: false
        }
    }

    override fun destroy() {
        purge()
    }

    override fun get(p0: Job?): TileBitmap? {
        return null
    }

    override fun getCapacity(): Int {
        return jobCache.maxSize()
    }

    override fun getCapacityFirstLevel(): Int {
        return jobCache.maxSize()
    }

    override fun getImmediately(p0: Job?): TileBitmap? {
        return null
    }

    override fun purge() {
        synchronized(lock) {
            jobCache.evictAll()
        }
    }

    override fun put(p0: Job?, p1: TileBitmap?) {
        synchronized(lock) {
            p0?.let { jobCache.put(it.key, true) }
        }
    }

    override fun setWorkingSet(p0: Set<Job?>?) {
        // Do nothing
    }

    override fun addObserver(p0: Observer?) {
        // Do nothing
    }

    override fun removeObserver(p0: Observer?) {
        // Do nothing
    }
}
