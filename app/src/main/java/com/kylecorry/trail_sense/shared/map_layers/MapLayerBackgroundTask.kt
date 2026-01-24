package com.kylecorry.trail_sense.shared.map_layers

import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

class MapLayerBackgroundTask {

    private var lastRunBounds: CoordinateBounds? = null
    private var lastRunMetersPerPixel: Float? = null
    private var lastQueuedBounds: CoordinateBounds? = null
    private var lastQueuedMetersPerPixel: Float? = null

    private val scope = CoroutineScope(Dispatchers.Default)
    private val runner = CoroutineQueueRunner(1, queuePolicy = BufferOverflow.DROP_OLDEST)
    private val lock = Mutex()
    private val taskLock = Any()
    private var isDirty = true

    private val tasks =
        mutableListOf<suspend (viewBounds: Rectangle, bounds: CoordinateBounds, projection: IMapViewProjection) -> Unit>()

    fun addTask(task: suspend (viewBounds: Rectangle, bounds: CoordinateBounds, projection: IMapViewProjection) -> Unit) {
        synchronized(taskLock) {
            tasks.add(task)
        }
        scope.launch {
            lock.withLock {
                isDirty = true
            }
        }
    }

    fun clearTasks() {
        synchronized(taskLock) {
            tasks.clear()
        }
        scope.launch {
            lock.withLock {
                isDirty = true
            }
        }
    }

    fun scheduleUpdate(
        viewBounds: Rectangle,
        bounds: CoordinateBounds,
        projection: IMapViewProjection,
        isInvalid: Boolean = false,
        update: suspend (viewBounds: Rectangle, bounds: CoordinateBounds, projection: IMapViewProjection) -> Unit = { viewBounds, bounds, projection ->
            val taskCopy = synchronized(taskLock) {
                tasks.toList()
            }
            Parallel.forEach(taskCopy.map { { it(viewBounds, bounds, projection) } })
        }
    ) {

        scope.launch {
            val metersPerPixel = projection.metersPerPixel

            lock.withLock {
                // If the bounds/meters per pixel have already been ran or queued, exit
                if (!isDirty && !isInvalid && areBoundsEqual(
                        bounds,
                        lastRunBounds ?: CoordinateBounds.empty
                    ) &&
                    metersPerPixel == lastRunMetersPerPixel
                ) {
                    return@launch
                }

                // This was causing it to not run when required, figure this out since this causes an extra re-render
//                if (!isInvalid && areBoundsEqual(
//                        newBounds,
//                        lastQueuedBounds ?: CoordinateBounds.empty
//                    ) &&
//                    metersPerPixel == lastQueuedMetersPerPixel
//                ) {
//                    return@launch
//                }

                val runPercentScaleDifference = lastRunMetersPerPixel?.let {
                    (abs(metersPerPixel - it) / it)
                } ?: 0f


                val queuedPercentScaleDifference = lastQueuedMetersPerPixel?.let {
                    (abs(metersPerPixel - it) / it)
                } ?: 0f

                val runPercentTranslateDifference = lastRunBounds?.let {
                    it.center.distanceTo(bounds.center) / bounds.width().meters().value
                } ?: 0f

                val queuedPercentTranslateDifference = lastQueuedBounds?.let {
                    it.center.distanceTo(bounds.center) / bounds.width().meters().value
                } ?: 0f

                // This will be queued up
                lastQueuedBounds = bounds
                lastQueuedMetersPerPixel = metersPerPixel
                isDirty = false

                // Otherwise queue it up
                val run = suspend {
                    lock.withLock {
                        lastRunBounds = bounds
                        lastRunMetersPerPixel = metersPerPixel
                    }
                    update(viewBounds, bounds, projection)
                }

                val scaleSignificantlyChanged =
                    runPercentScaleDifference > 0.4f && queuedPercentScaleDifference > 0.01f
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

    private fun areBoundsEqual(bounds1: CoordinateBounds, bound2: CoordinateBounds): Boolean {
        return bounds1.north == bound2.north &&
                bounds1.south == bound2.south &&
                bounds1.east == bound2.east &&
                bounds1.west == bound2.west
    }

}