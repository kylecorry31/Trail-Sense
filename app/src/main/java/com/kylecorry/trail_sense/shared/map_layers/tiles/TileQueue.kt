package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.util.Log
import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import java.util.PriorityQueue
import kotlin.math.log

class TileQueue {
    // Enable this to log what tiles are being loaded
    private val shouldLog = false
    private var changeListener: (tile: ImageTile) -> Unit = {}
    private val loadingKeys = mutableSetOf<String>()

    private val comparator = compareBy<ImageTile> { getPriority(it) }

    private val queue = PriorityQueue(11, comparator)
    private val queuedKeys = mutableSetOf<String>()

    private var mapProjection: IMapViewProjection? = null
    private var desiredTiles: Set<Tile>? = null

    fun setMapProjection(projection: IMapViewProjection) {
        mapProjection = projection

        // Reprioritize the queue
        synchronized(queue) {
            val previous = queue.toList()
            queue.clear()
            queue.addAll(previous)
        }
    }

    fun setDesiredTiles(tiles: List<Tile>) {
        desiredTiles = tiles.toSet()
    }

    fun enqueue(tile: ImageTile) {
        val state = tile.state
        if (state != TileState.Idle && state != TileState.Stale) {
            return
        }
        synchronized(loadingKeys) {
            if (!loadingKeys.contains(tile.key)) {
                synchronized(queue) {
                    if (queuedKeys.add(tile.key)) {
                        queue.add(tile)
                    }
                }
            }
        }
    }

    fun clear() {
        synchronized(queue) {
            queue.clear()
            queuedKeys.clear()
        }
        synchronized(loadingKeys) {
            loadingKeys.clear()
        }
    }

    fun count(): Int {
        return synchronized(queue) {
            queue.size
        }
    }

    private fun dequeue(): ImageTile? {
        return synchronized(queue) {
            queue.poll()?.also { queuedKeys.remove(it.key) }
        }
    }

    suspend fun load(maxTotalLoads: Int, maxNewLoads: Int = maxTotalLoads) {
        val jobs = mutableListOf<ImageTile>()
        val tiles = desiredTiles ?: return
        while (getLoadingCount() < maxTotalLoads && jobs.size < maxNewLoads && count() > 0) {
            val tile = dequeue() ?: continue
            if (tile.tile !in tiles) {
                // This tile is no longer wanted
                continue
            }

            val key = tile.key
            val tileState = tile.state
            if (tileState == TileState.Idle || tileState == TileState.Stale) {
                val shouldLoad = synchronized(loadingKeys) {
                    loadingKeys.add(key)
                }
                if (shouldLoad) {
                    jobs.add(tile)
                }
            }
        }
        // TODO: Allow cancellation of a job - likely means storing the actual jobs somewhere
        try {
            Parallel.forEach(jobs) {
                val start = System.currentTimeMillis()
                it.load()
                val end = System.currentTimeMillis()
                if (shouldLog) {
                    Log.d("TileQueue", "${it.key} (${end - start}ms)")
                }
                onStateChange(it)
            }
        } finally {
            synchronized(loadingKeys) {
                jobs.forEach { loadingKeys.remove(it.key) }
            }
        }
    }

    private fun onStateChange(tile: ImageTile) {
        synchronized(loadingKeys) {
            loadingKeys.remove(tile.key)
        }
        changeListener(tile)
    }

    fun getLoadingCount(): Int {
        return synchronized(loadingKeys) {
            loadingKeys.size
        }
    }

    private fun getPriority(tile: ImageTile): Double {
        val projection = mapProjection ?: return 0.0
        val center = projection.toPixels(projection.center)
        val tileCenter = projection.toPixels(tile.tile.getCenter())
        val distance = center.distanceTo(tileCenter)
        val resolution = tile.tile.getResolution()
        // https://github.com/openlayers/openlayers/blob/main/src/ol/TileQueue.js
        return 65536 * log(resolution, 10.0) + distance / resolution
    }

    fun setChangeListener(listener: (tile: ImageTile) -> Unit) {
        changeListener = listener
    }

}
