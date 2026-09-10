package com.kylecorry.trail_sense.shared.background

import com.kylecorry.luna.subscriptions.Subscription
import com.kylecorry.luna.time.ITimer
import com.kylecorry.luna.topics.ITopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
            val topic = topicProvider(periodMillis)
            val bus = Subscription()
            bus.subscribe(action)
            try {
                if (delayMillis > 0) {
                    delay(delayMillis.milliseconds)
                }
                topic.read {
                    bus.publish()
                    isOneTime || !isActive
                }
            } finally {
                bus.unsubscribe(action)
                topic.unsubscribeAll()
            }
        }
    }
}
