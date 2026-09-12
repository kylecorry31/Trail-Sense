package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.luna.concurrency.IFlowable
import com.kylecorry.luna.time.FlowableTimer
import com.kylecorry.luna.topics.BaseTopic
import com.kylecorry.luna.topics.Topic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class FlowableTimerTest {

    private val topics = TestTopicProvider()
    private val runs = AtomicInteger()

    private fun timer(action: suspend () -> Unit = { runs.incrementAndGet() }): FlowableTimer {
        return FlowableTimer(topics, action = action)
    }

    @Test
    fun runsImmediatelyThenKeepsOneSubscriptionBetweenActions() = runBlocking {
        val timer = timer()
        try {
            timer.interval(15000)
            awaitRuns(1)
            val topic = topics.awaitTopic()
            assertTrue(timer.isRunning())
            assertEquals(listOf(15000L), topics.periods)
            assertNoMoreRuns(1)

            // The topic controls the cadence, even when publishes are less than a period apart.
            topic.publish()
            assertNoMoreRuns(1)
            repeat(3) { index ->
                topic.publish()
                awaitRuns(index + 2)
                assertTrue(topic.isSubscribed)
                assertEquals(1, topic.subscriptions.get())
            }
        } finally {
            timer.stop()
        }
        waitFor("timer did not unsubscribe") { !topics.current!!.isSubscribed }
    }

    @Test
    fun unregistersWhileTheActionRuns() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val canFinish = CompletableDeferred<Unit>()
        val timer = FlowableTimer(topics, unregisterWhileRunning = true) {
            started.complete(Unit)
            canFinish.await()
            runs.incrementAndGet()
        }
        try {
            timer.interval(1000, 1000)
            val topic = topics.awaitTopic()
            topic.publish()
            assertNoMoreRuns(0)
            topic.publish()
            waitFor("action did not start") { started.isCompleted }
            waitFor("timer did not unsubscribe while action was running") { !topic.isSubscribed }

            repeat(10) { topic.publish() }
            assertEquals(0, runs.get())
            canFinish.complete(Unit)
            awaitRuns(1)
            assertNoMoreRuns(1)
        } finally {
            timer.stop()
        }
    }

    @Test
    fun theInitialDelayIsDrivenByItsOwnTopic() = runBlocking {
        val timer = timer()
        try {
            timer.interval(1000, 200)
            val delayTopic = topics.awaitTopic()
            assertEquals(listOf(200L), topics.periods)

            // The publish which arrives on subscribe starts the delay rather than ending it
            delayTopic.publish()
            assertNoMoreRuns(0)
            delayTopic.publish()
            awaitRuns(1)
            waitFor("delay topic was left subscribed") { !delayTopic.isSubscribed }

            val periodTopic = topics.awaitTopic()
            assertEquals(listOf(200L, 1000L), topics.periods)
            periodTopic.publish()
            assertNoMoreRuns(1)
            periodTopic.publish()
            awaitRuns(2)
        } finally {
            timer.stop()
        }
    }

    @Test
    fun theInitialDelayReusesTheTopicWhenItMatchesThePeriod() = runBlocking {
        val timer = timer()
        try {
            timer.interval(1000, 1000)
            val topic = topics.awaitTopic()
            assertEquals(listOf(1000L), topics.periods)

            topic.publish()
            assertNoMoreRuns(0)
            repeat(2) { index ->
                topic.publish()
                awaitRuns(index + 1)
            }
            assertEquals(listOf(1000L), topics.periods)
            assertEquals(1, topic.subscriptions.get())
        } finally {
            timer.stop()
        }
    }

    @Test
    fun onceStopsListeningAfterTheFirstPublishFollowingTheDelay() = runBlocking {
        val timer = timer()
        try {
            timer.once(50)
            val topic = topics.awaitTopic()
            assertEquals(listOf(50L), topics.periods)
            topic.publish()
            assertNoMoreRuns(0)
            assertTrue(timer.isRunning())

            topic.publish()
            waitFor("one-shot timer kept running") { !timer.isRunning() }
            awaitRuns(1)
            waitFor("one-shot timer did not unsubscribe") { !topic.isSubscribed }

            repeat(3) { topic.publish() }
            assertNoMoreRuns(1)
        } finally {
            timer.stop()
        }
    }

    @Test
    fun onceWithZeroDelayRunsImmediatelyWithoutCreatingATopic() = runBlocking {
        val timer = timer()
        try {
            timer.once(0)
            awaitRuns(1)
            assertEquals(emptyList<Long>(), topics.periods)
            assertFalse(timer.isRunning())
        } finally {
            timer.stop()
        }
    }

    @Test
    fun intervalWithZeroInitialDelayRunsImmediatelyThenListensForThePeriod() = runBlocking {
        val timer = timer()
        try {
            timer.interval(1000, 0)
            awaitRuns(1)

            val topic = topics.awaitTopic()
            assertEquals(listOf(1000L), topics.periods)
            topic.publish()
            assertNoMoreRuns(1)
            topic.publish()
            awaitRuns(2)
        } finally {
            timer.stop()
        }
    }

    @Test
    fun restartingCreatesANewTopicAndStopsTheOldOne() = runBlocking {
        val timer = timer()
        try {
            timer.interval(1000)
            awaitRuns(1)
            val oldTopic = topics.awaitTopic()
            timer.interval(2000)
            awaitRuns(2)
            waitFor("old topic was left subscribed") { !oldTopic.isSubscribed }
            val topic = topics.awaitTopic()
            assertEquals(listOf(1000L, 2000L), topics.periods)

            oldTopic.publish()
            assertNoMoreRuns(2)
            topic.publish()
            assertNoMoreRuns(2)
            topic.publish()
            awaitRuns(3)
        } finally {
            timer.stop()
        }
    }

    @Test
    fun publishesAfterStopAreIgnored() = runBlocking {
        val timer = timer()
        timer.interval(1000)
        awaitRuns(1)
        val topic = topics.awaitTopic()
        timer.stop()
        waitFor("timer did not unsubscribe") { !topic.isSubscribed }
        assertFalse(timer.isRunning())
        topic.publish()
        assertNoMoreRuns(1)
    }

    @Test
    fun stoppingDuringTheInitialDelayUnsubscribes() = runBlocking {
        val timer = timer()
        timer.interval(1000, 200)
        val topic = topics.awaitTopic()
        timer.stop()
        waitFor("timer did not unsubscribe") { !topic.isSubscribed }
        assertFalse(timer.isRunning())
        repeat(2) { topic.publish() }
        assertNoMoreRuns(0)
        assertEquals(listOf(200L), topics.periods)
    }

    @Test
    fun stoppingDuringTheActionCancelsItAndThePendingUpdate() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val timer = timer {
            started.complete(Unit)
            try {
                CompletableDeferred<Unit>().await()
                runs.incrementAndGet()
            } finally {
                cancelled.complete(Unit)
            }
        }
        try {
            timer.interval(1000, 1000)
            val topic = topics.awaitTopic()
            topic.publish()
            assertNoMoreRuns(0)
            topic.publish()
            waitFor("action did not start") { started.isCompleted }
            topic.publish()
            timer.stop()
            waitFor("action was not cancelled") { cancelled.isCompleted }
            waitFor("timer did not unsubscribe") { !topic.isSubscribed }
            assertNoMoreRuns(0)
        } finally {
            timer.stop()
        }
    }

    @Test
    fun acceptsATopicDirectly() = runBlocking {
        val topic = TestTopic()
        val timer = FlowableTimer(topic) { runs.incrementAndGet() }
        try {
            timer.interval(1000)
            awaitRuns(1)
            waitFor("timer did not subscribe") { topic.isSubscribed }
            topic.publish()
            assertNoMoreRuns(1)
            topic.publish()
            awaitRuns(2)
        } finally {
            timer.stop()
        }
        waitFor("timer did not unsubscribe") { !topic.isSubscribed }
    }

    private suspend fun awaitRuns(expected: Int) {
        waitFor("expected $expected runs, but there were ${runs.get()}") { runs.get() >= expected }
        assertEquals(expected, runs.get())
    }

    private suspend fun assertNoMoreRuns(expected: Int) {
        delay(50)
        assertEquals(expected, runs.get())
    }

    private class TestTopic : BaseTopic() {
        @Volatile
        private var subscriberCount = 0
        val subscriptions = AtomicInteger()

        override val topic = Topic(
            onSubscriberAdded = { count, _ ->
                subscriptions.incrementAndGet()
                subscriberCount = count
            },
            onSubscriberRemoved = { count, _ -> subscriberCount = count }
        )

        val isSubscribed: Boolean
            get() = subscriberCount > 0

        fun publish() = topic.publish()
    }

    private class TestTopicProvider : (Long) -> IFlowable<*> {
        val periods = CopyOnWriteArrayList<Long>()

        @Volatile
        var current: TestTopic? = null
            private set

        override fun invoke(periodMillis: Long): IFlowable<*> {
            periods.add(periodMillis)
            return TestTopic().also { current = it }
        }

        suspend fun awaitTopic(): TestTopic {
            waitFor("timer did not subscribe to a topic") { current?.isSubscribed == true }
            return current!!
        }
    }
}

private suspend fun waitFor(message: String, condition: () -> Boolean) {
    withTimeoutOrNull(2000) {
        while (!condition()) {
            delay(1)
        }
    } ?: fail(message)
}
