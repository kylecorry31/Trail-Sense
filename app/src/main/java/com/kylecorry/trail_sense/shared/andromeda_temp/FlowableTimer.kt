package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.luna.concurrency.IFlowable
import com.kylecorry.luna.time.ITimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * A timer driven by flowable emissions after an optional initial delay.
 *
 * @param flowableProvider a factory function for the flowable
 * @param action the action to perform on each timer tick
 */
class FlowableTimer(
    private val flowableProvider: (periodMillis: Long) -> IFlowable<*>,
    private val action: suspend () -> Unit
) : ITimer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val lock = Any()

    @Volatile
    private var job: Job? = null

    constructor(
        flowable: IFlowable<*>,
        action: suspend () -> Unit
    ) : this({ flowable }, action)

    override fun interval(period: Duration, initialDelay: Duration) {
        interval(period.toMillis(), initialDelay.toMillis())
    }

    override fun interval(periodMillis: Long, initialDelayMillis: Long) {
        require(periodMillis > 0)
        require(initialDelayMillis >= 0)
        start(periodMillis, initialDelayMillis, isOneTime = false)
    }

    override fun once(delay: Duration) {
        once(delay.toMillis())
    }

    /** The delay doubles as the rate of the flowable, since a one time timer has no period. */
    override fun once(delayMillis: Long) {
        require(delayMillis >= 0)
        start(delayMillis, delayMillis, isOneTime = true)
    }

    override fun stop() {
        synchronized(lock) { job?.cancel() }
    }

    override fun isRunning(): Boolean = job?.isActive == true

    private fun start(
        periodMillis: Long,
        delayMillis: Long,
        isOneTime: Boolean
    ) = synchronized(lock) {
        job?.cancel()
        job = scope.launch {
            if (delayMillis == 0L) {
                action()
                if (isOneTime) {
                    return@launch
                }
            }
            // If the delayMillis is not the period, then a new listener is needed since a listener can't change periods
            if (!isOneTime && delayMillis != 0L && delayMillis != periodMillis) {
                listen(delayMillis, skipFirst = true, stopAfterTick = true)
            }
            // Always skip the first tick since it is either the immediate action or the initial delay action above
            listen(periodMillis, skipFirst = true, stopAfterTick = isOneTime)
        }
    }

    private suspend fun listen(rateMillis: Long, skipFirst: Boolean, stopAfterTick: Boolean) {
        val flowable = flowableProvider(rateMillis)
        var ticks = flowable.flow
        if (skipFirst) {
            ticks = ticks.drop(1)
        }
        if (stopAfterTick) {
            ticks.first()
            action()
        } else {
            ticks.collect { action() }
        }
    }
}
