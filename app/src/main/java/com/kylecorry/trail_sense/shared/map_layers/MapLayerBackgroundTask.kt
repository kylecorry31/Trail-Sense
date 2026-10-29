package com.kylecorry.trail_sense.shared.map_layers

import android.content.Context
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.concurrency.Parallel
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

class MapLayerBackgroundTask {

    private class RunKey(val bounds: CoordinateBounds, val zoom: Int)

    @Volatile
    private var lastRun: RunKey? = null
    private var lastQueuedBounds: CoordinateBounds? = null
    private var lastQueuedZoom: Int? = null

    private val scope = CoroutineScope(Dispatchers.Default)
    private val runner = CoroutineQueueRunner(1, queuePolicy = BufferOverflow.DROP_OLDEST)
    private val lock = Mutex()
    private val taskLock = Any()
    @Volatile
    private var isDirty = true

    private val tasks =
        mutableListOf<suspend (context: Context, viewBounds: Rectangle, bounds: CoordinateBounds, projection: IMapViewProjection) -> Unit>()

    fun addTask(task: suspend (context: Context, viewBounds: Rectangle, bounds: CoordinateBounds, projection: IMapViewProjection) -> Unit) {
        synchronized(taskLock) {
            tasks.add(task)
        }
        scope.launch {
            lock.withLock {
                isDirty = true
            }
        }
    }

    fun scheduleUpdate(
        context: Context,
        viewBounds: Rectangle,
        bounds: CoordinateBounds,
        projection: IMapViewProjection,
        isInvalid: Boolean = false,
        snapToTiles: Boolean = true,
        update: suspend (context: Context, viewBounds: Rectangle, bounds: CoordinateBounds, projection: IMapViewProjection) -> Unit = { context, viewBounds, bounds, projection ->
            val taskCopy = synchronized(taskLock) {
                tasks.toList()
            }
            Parallel.forEach(taskCopy.map { { it(context, viewBounds, bounds, projection) } })
        }
    ) {
        val zoom = projection.zoom.roundToInt()
        val newBounds = if (snapToTiles) {
            TileMath.snapToTiles(bounds, zoom)
        } else {
            bounds
        }

        // This runs on every draw, so avoid launching a coroutine when nothing has changed
        if (isUpToDate(newBounds, zoom, isInvalid)) {
            return
        }

        scope.launch {
            lock.withLock {
                // If the bounds/meters per pixel have already been ran or queued, exit
                if (isUpToDate(newBounds, zoom, isInvalid)) {
                    return@launch
                }

                // This was causing it to not run when required, figure this out since this causes an extra re-render
//                if (!isInvalid && areBoundsEqual(
//                        newBounds,
//                        lastQueuedBounds ?: CoordinateBounds.empty
//                    ) &&
//                    zoom == lastQueuedZoom
//                ) {
//                    return@launch
//                }

                val runPercentScaleDifferent = lastRun?.let {
                    it.zoom != zoom
                } ?: false


                val queuedPercentScaleDifferent = lastQueuedZoom?.let {
                    it != zoom
                } ?: false

                val runPercentTranslateDifference = lastRun?.let {
                    it.bounds.center.distanceTo(newBounds.center) / newBounds.width().meters().value
                } ?: 0f

                val queuedPercentTranslateDifference = lastQueuedBounds?.let {
                    it.center.distanceTo(newBounds.center) / newBounds.width().meters().value
                } ?: 0f

                // This will be queued up
                lastQueuedBounds = newBounds
                lastQueuedZoom = zoom
                isDirty = false

                // Otherwise queue it up
                val run = suspend {
                    lock.withLock {
                        lastRun = RunKey(newBounds, zoom)
                    }
                    update(context, viewBounds, newBounds, projection)
                }

                val scaleSignificantlyChanged =
                    runPercentScaleDifferent && queuedPercentScaleDifferent
                val translateSignificantlyChanged =
                    runPercentTranslateDifference > 0.4f && queuedPercentTranslateDifference > 0.01f

                if (scaleSignificantlyChanged || translateSignificantlyChanged) {
                    runner.replace(run)
                } else {
                    runner.enqueue(run)
                }
            }
        }
    }

    fun stop() {
        runner.cancel()
    }

    private fun isUpToDate(bounds: CoordinateBounds, zoom: Int, isInvalid: Boolean): Boolean {
        val lastRun = lastRun ?: return false
        return !isDirty && !isInvalid && zoom == lastRun.zoom && areBoundsEqual(
            bounds,
            lastRun.bounds
        )
    }

    private fun areBoundsEqual(bounds1: CoordinateBounds, bound2: CoordinateBounds): Boolean {
        return bounds1.north == bound2.north &&
                bounds1.south == bound2.south &&
                bounds1.east == bound2.east &&
                bounds1.west == bound2.west
    }

}
