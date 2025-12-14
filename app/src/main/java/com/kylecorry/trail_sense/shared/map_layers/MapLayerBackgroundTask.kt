package com.kylecorry.trail_sense.shared.map_layers

import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.luna.coroutines.ParallelCoroutineRunner
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.andromeda_temp.ParallelCoroutineRunner2
import com.kylecorry.trail_sense.shared.andromeda_temp.grow
import com.kylecorry.trail_sense.shared.device.DeviceSubsystem
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
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
    private val growPercent = getGrowPercent()

    private val tasks =
        mutableListOf<suspend (bounds: CoordinateBounds, metersPerPixel: Float) -> Unit>()

    fun addTask(task: suspend (bounds: CoordinateBounds, metersPerPixel: Float) -> Unit) {
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
        bounds: CoordinateBounds,
        metersPerPixel: Float,
        isInvalid: Boolean = false,
        update: suspend (bounds: CoordinateBounds, metersPerPixel: Float) -> Unit = { bounds, metersPerPixel ->
            val taskCopy = synchronized(taskLock) {
                tasks.toList()
            }
            val parallel = ParallelCoroutineRunner2()
            parallel.run(taskCopy.map { { it(bounds, metersPerPixel) } })
        }
    ) {

        scope.launch {
            val newBounds =
                TileMath.snapToTiles(bounds.grow(growPercent), metersPerPixel.toDouble())

            lock.withLock {
                // If the bounds/meters per pixel have already been ran or queued, exit
                if (!isDirty && !isInvalid && areBoundsEqual(
                        newBounds,
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
                    it.center.distanceTo(newBounds.center) / newBounds.width().meters().value
                } ?: 0f

                val queuedPercentTranslateDifference = lastQueuedBounds?.let {
                    it.center.distanceTo(newBounds.center) / newBounds.width().meters().value
                } ?: 0f

                // This will be queued up
                lastQueuedBounds = newBounds
                lastQueuedMetersPerPixel = metersPerPixel
                isDirty = false

                // Otherwise queue it up
                val run = suspend {
                    lock.withLock {
                        lastRunBounds = newBounds
                        lastRunMetersPerPixel = metersPerPixel
                    }
                    update(newBounds, metersPerPixel)
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

    private fun getGrowPercent(): Float {
        val device = AppServiceRegistry.get<DeviceSubsystem>()
        val threshold = 50 * 1024 * 1024 // 50 MB
        return if (device.getAvailableBitmapMemoryBytes() < threshold) {
            0f
        } else {
            0.2f
        }
    }

    fun stop() {
        runner.cancel()
        clearTasks()
    }

    private fun areBoundsEqual(bounds1: CoordinateBounds, bound2: CoordinateBounds): Boolean {
        return bounds1.north == bound2.north &&
                bounds1.south == bound2.south &&
                bounds1.east == bound2.east &&
                bounds1.west == bound2.west
    }

}