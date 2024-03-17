package com.kylecorry.trail_sense.shared.hooks

import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

class StateManager(
    observeOn: CoroutineDispatcher,
    throttleTimeMs: Long,
    private val onChange: () -> Unit
) {

    private val scope = CoroutineScope(Dispatchers.Default)

    private var lastUpdateTime = 0L
    private var lastTriggerTime = 0L

    private val queue = CoroutineQueueRunner(dispatcher = observeOn)
    private val throttle = Throttle(throttleTimeMs)

    private fun execute() {
        if (lastUpdateTime > lastTriggerTime) {
            lastTriggerTime = System.currentTimeMillis()
            onChange()
        }
    }

    fun start() {
        lastUpdateTime = System.currentTimeMillis()
    }

    fun stop() {
        queue.cancel()
    }

    fun <T> state(initialValue: T): State<T> {
        return State(initialValue, this::scheduleChange)
    }

    private fun scheduleChange() {
        lastUpdateTime = System.currentTimeMillis()
        addToQueue()
    }

    private fun addToQueue() {
        scope.launch {
            queue.enqueue {
                if (throttle.isThrottled()) {
                    addToQueue()
                    return@enqueue
                }
                execute()
            }
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