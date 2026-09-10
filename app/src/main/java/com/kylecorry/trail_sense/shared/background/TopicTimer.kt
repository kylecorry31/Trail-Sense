package com.kylecorry.trail_sense.shared.background

import com.kylecorry.luna.time.ITimer
import com.kylecorry.luna.topics.ITopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * A timer driven by topic publishes after an optional initial delay.
 *
 * @param topicProvider a factory function for the topic
 * @param action the action to perform on each timer tick
 */
class TopicTimer(
    private val topicProvider: (periodMillis: Long) -> ITopic,
    private val action: suspend () -> Unit
) : ITimer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val lock = Any()

    @Volatile
    private var job: Job? = null

    constructor(
        topic: ITopic,
        action: suspend () -> Unit
    ) : this({ topic }, action)

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

    /** The delay doubles as the rate of the topic, since a one time timer has no period. */
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
        val topic = topicProvider(rateMillis)
        var ticks = topic.flow
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
