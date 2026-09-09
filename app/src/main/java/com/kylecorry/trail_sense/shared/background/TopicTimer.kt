package com.kylecorry.trail_sense.shared.background

import com.kylecorry.luna.time.ITimer
import com.kylecorry.luna.topics.ITopic
import com.kylecorry.trail_sense.shared.andromeda_temp.SystemTimeProvider
import com.kylecorry.trail_sense.shared.andromeda_temp.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * A timer driven by a topic publish event. Assuming the topic fires at a regular interval, the gap between the end of one action
 * and the start of the next will be roughly the period of the topic.
 *
 * @param topicProvider a factory function for the topic
 * @param timeProvider a provider for the current time
 * @param toleranceMillis how much earlier a publish can arrive and still trigger the action.
 * @param action the action to perform on each timer tick
 */
class TopicTimer(
    private val topicProvider: (periodMillis: Long) -> ITopic,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val toleranceMillis: Long = Long.MAX_VALUE,
    private val action: suspend () -> Unit
) : ITimer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val lock = Any()

    @Volatile
    private var job: Job? = null

    constructor(
        topic: ITopic,
        timeProvider: TimeProvider = SystemTimeProvider(),
        toleranceMillis: Long = Long.MAX_VALUE,
        action: suspend () -> Unit
    ) : this({ topic }, timeProvider, toleranceMillis, action)

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
            val topic = lazy { topicProvider(periodMillis) }
            var nextRunAt = timeProvider.elapsedRealtime() + delayMillis
            do {
                if (timeProvider.elapsedRealtime() < nextRunAt) {
                    topic.value.read { nextRunAt - timeProvider.elapsedRealtime() <= toleranceMillis }
                }
                action()
                nextRunAt = timeProvider.elapsedRealtime() + periodMillis
            } while (!isOneTime)
        }
    }
}
