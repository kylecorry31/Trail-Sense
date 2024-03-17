package com.kylecorry.trail_sense.shared.hooks

import com.kylecorry.andromeda.core.time.CoroutineTimer
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.reflect.KProperty

class StateManager(
    observeOn: CoroutineDispatcher,
    private val throttleTimeMs: Long,
    private val onChange: () -> Unit
) {
    private var lastUpdateTime = 0L

    private val timer = CoroutineTimer(observeOn = observeOn) {
        synchronized(lock) {
            hasPendingUpdate = false
            lastUpdateTime = System.currentTimeMillis()
        }
        onChange()
    }

    private var hasPendingUpdate = false
    private val lock = Any()


    fun start() {
        synchronized(lock) {
            hasPendingUpdate = true
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
            if (hasPendingUpdate) {
                // There's already an update scheduled
                return
            }

            // Otherwise, an update needs to be scheduled, ensuring that it's throttled
            val timeSinceLastUpdate = System.currentTimeMillis() - lastUpdateTime
            val timeToNextUpdate = if (timeSinceLastUpdate < throttleTimeMs) {
                throttleTimeMs - timeSinceLastUpdate
            } else {
                0
            }.coerceIn(0, throttleTimeMs)

            hasPendingUpdate = true
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