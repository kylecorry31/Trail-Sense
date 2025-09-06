package com.kylecorry.trail_sense.shared.canvas

import com.kylecorry.sol.science.geology.CoordinateBounds
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
    private val runner = CoroutineQueueRunner2(1, queuePolicy = BufferOverflow.DROP_OLDEST)
    private val lock = Mutex()
    private val taskLock = Any()

    private val tasks =
        mutableListOf<suspend (bounds: CoordinateBounds, metersPerPixel: Float) -> Unit>()

    fun addTask(task: suspend (bounds: CoordinateBounds, metersPerPixel: Float) -> Unit) {
        synchronized(taskLock) {
            tasks.add(task)
        }
    }

    fun clearTasks() {
        synchronized(taskLock) {
            tasks.clear()
        }
    }

    fun scheduleUpdate(
        bounds: CoordinateBounds,
        metersPerPixel: Float,
        isInvalid: Boolean = false,
        update: suspend (bounds: CoordinateBounds, metersPerPixel: Float) -> Unit = { bounds, metersPerPixel ->
            val taskCopy = synchronized(taskLock){
                tasks.toList()
            }
            for (task in taskCopy) {
                task(bounds, metersPerPixel)
            }
        }
    ) {
        scope.launch {
            lock.withLock {
                // If the bounds/meters per pixel have already been ran or queued, exit
                if (!isInvalid && areBoundsEqual(
                        bounds,
                        lastRunBounds ?: CoordinateBounds.Companion.empty
                    ) &&
                    metersPerPixel == lastRunMetersPerPixel
                ) {
                    return@launch
                }

                // This was causing it to not run when required, figure this out since this causes an extra re-render
//                if (!isInvalid && areBoundsEqual(
//                        bounds,
//                        lastQueuedBounds ?: CoordinateBounds.Companion.empty
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
                    it.center.distanceTo(bounds.center) / bounds.width().meters().distance
                } ?: 0f

                val queuedPercentTranslateDifference = lastQueuedBounds?.let {
                    it.center.distanceTo(bounds.center) / bounds.width().meters().distance
                } ?: 0f

                // This will be queued up
                lastQueuedBounds = bounds
                lastQueuedMetersPerPixel = metersPerPixel

                // Otherwise queue it up
                val run = suspend {
                    lock.withLock {
                        lastRunBounds = bounds
                        lastRunMetersPerPixel = metersPerPixel
                    }
                    update(bounds, metersPerPixel)
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

    fun cancelUpdates() {
        runner.cancel()
    }

    private fun areBoundsEqual(bounds1: CoordinateBounds, bound2: CoordinateBounds): Boolean {
        return bounds1.north == bound2.north &&
                bounds1.south == bound2.south &&
                bounds1.east == bound2.east &&
                bounds1.west == bound2.west
    }

}