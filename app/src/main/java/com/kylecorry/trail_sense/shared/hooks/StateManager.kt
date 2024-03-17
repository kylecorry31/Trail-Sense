package com.kylecorry.trail_sense.shared.hooks

import com.kylecorry.andromeda.core.time.CoroutineTimer
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.math.max
import kotlin.reflect.KProperty

class StateManager(
    observeOn: CoroutineDispatcher,
    private val throttleTimeMs: Long,
    private val onChange: () -> Unit
) {
    private var lastUpdateTime = 0L
    private var lastTriggerTime = 0L

    private val timer = CoroutineTimer(observeOn = observeOn) {
        synchronized(lock) {
            lastTriggerTime = System.currentTimeMillis()
        }
        onChange()
    }

    private var nextScheduledTime = 0L
    private val lock = Any()


    fun start() {
        synchronized(lock) {
            lastUpdateTime = System.currentTimeMillis()
            nextScheduledTime = lastUpdateTime
            timer.once(0)
        }
    }

    fun stop() {
        timer.stop()
    }

    fun <T> state(initialValue: T): State<T> {
        return State(initialValue, this::scheduleChange)
    }

    private fun scheduleChange() {
        synchronized(lock) {
            lastUpdateTime = System.currentTimeMillis()
            if (nextScheduledTime > lastUpdateTime) {
                // There's already an update scheduled
                return
            }

            // Otherwise, an update needs to be scheduled, ensuring that it's throttled
            val timeSinceLastUpdate =
                System.currentTimeMillis() - max(lastTriggerTime, nextScheduledTime)
            val timeToNextUpdate = if (timeSinceLastUpdate < throttleTimeMs) {
                throttleTimeMs - timeSinceLastUpdate.coerceAtLeast(0)
            } else {
                0
            }

            nextScheduledTime = System.currentTimeMillis() + timeToNextUpdate
            timer.once(timeToNextUpdate)
        }
    }
}

class State<T>(
    initialValue: T,
    private val onChange: () -> Unit
) {

    private var value: T = initialValue

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val hasChanges = this.value != value
        this.value = value
        if (hasChanges) {
            onChange()
        }
    }

}