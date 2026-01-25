package com.kylecorry.trail_sense.shared.map_layers.tiles

import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import java.util.PriorityQueue
import kotlin.math.log

class TileQueue {

    private var changeListener: (tile: ImageTile) -> Unit = {}
    private val loadingKeys = mutableSetOf<String>()

    private val comparator = compareBy<ImageTile> { getPriority(it) }

    private val queue = PriorityQueue(11, comparator)

    private var mapProjection: IMapViewProjection? = null
    private var mapBounds = CoordinateBounds.empty

    fun setMapState(projection: IMapViewProjection, bounds: CoordinateBounds) {
        mapProjection = projection
        mapBounds = bounds

        // Reprioritize the queue
        synchronized(queue) {
            val previous = queue.toList()
            queue.clear()
            queue.addAll(previous)
        }
    }

    fun enqueue(tile: ImageTile) {
        synchronized(loadingKeys) {
            if (!loadingKeys.contains(tile.key)) {
                synchronized(queue) {
                    queue.add(tile)
                }
            }
        }
    }

    fun clear() {
        synchronized(queue) {
            queue.clear()
        }
    }

    fun count(): Int {
        return synchronized(queue) {
            queue.size
        }
    }

    private fun dequeue(): ImageTile? {
        return synchronized(queue) {
            queue.poll()
        }
    }

    suspend fun load(maxTotalLoads: Int, maxNewLoads: Int) {
        val jobs = mutableListOf<ImageTile>()
        val projection = mapProjection ?: return
        val z = TileMath.getZoomLevel(mapBounds, projection.metersPerPixel)
        val desiredTiles = TileMath.getTiles(mapBounds, z).toSet()
        while (getLoadingCount() < maxTotalLoads && jobs.size < maxNewLoads && count() > 0) {
            val tile = dequeue() ?: continue
            if (tile.tile !in desiredTiles) {
                // This tile is no longer wanted
                continue
            }

            val key = tile.key
            val tileState = tile.state
            if (tileState == TileState.Idle) {
                val shouldLoad = synchronized(loadingKeys) {
                    loadingKeys.add(key)
                }
                if (shouldLoad) {
                    jobs.add(tile)
                }
            }
        }
        // TODO: Allow cancellation of a job - likely means storing the actual jobs somewhere
        Parallel.forEach(jobs) {
            it.load()
            onStateChange(it)
        }
    }

    private fun onStateChange(tile: ImageTile) {
        if (tile.state == TileState.Loaded || tile.state == TileState.Error || tile.state == TileState.Empty) {
            synchronized(loadingKeys) {
                loadingKeys.remove(tile.key)
            }
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