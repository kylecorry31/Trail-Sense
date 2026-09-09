package com.kylecorry.trail_sense.shared.background

import com.kylecorry.luna.topics.BaseTopic
import com.kylecorry.luna.topics.ITopic
import com.kylecorry.luna.topics.Topic
import com.kylecorry.trail_sense.shared.andromeda_temp.TimeProvider
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

class TopicTimerTest {

    private val topics = TestTopicProvider()
    private val time = FakeTimeProvider()
    private val runs = AtomicInteger()

    /** Defaults to no tolerance, so that the tests can pin down exactly which publish runs the action. */
    private fun timer(
        toleranceMillis: Long = 0,
        action: suspend () -> Unit = { runs.incrementAndGet() }
    ): TopicTimer {
        return TopicTimer(topics, time, toleranceMillis, action)
    }

    @Test
    fun runsTheActionOnEveryPublishAfterThePeriod() = runBlocking {
        val timer = timer()

        timer.interval(1000)

        assertTrue(timer.isRunning())
        awaitRuns(1)
        assertEquals(listOf(1000L), topics.periods)

        // Publishes before the period has elapsed are ignored
        val topic = topics.awaitTopic()
        topic.publishAt(999)
        assertNoMoreRuns(1)

        topic.publishAt(1000)
        awaitRuns(2)

        timer.stop()
        assertFalse(timer.isRunning())
        waitFor("timer did not unsubscribe") { !topic.isSubscribed }
    }

    @Test
    fun doesNotQueuePublishesWhileTheActionRuns() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val canFinish = CompletableDeferred<Unit>()
        val timer = timer {
            started.complete(Unit)
            canFinish.await()
            runs.incrementAndGet()
        }

        // The initial delay holds the first run until the timer is waiting on the topic
        timer.interval(1000, 1000)
        val topic = topics.awaitTopic()
        topic.publishAt(1000)
        awaitStart(started)

        // The timer unsubscribes before running the action, so publishes can't queue up
        assertFalse(topic.isSubscribed)
        topic.publish()

        // The action runs long enough to cover the next period, which starts when it completes
        time.elapsedMillis = 5000
        canFinish.complete(Unit)
        awaitRuns(1)

        topic.publishAt(5000)
        assertNoMoreRuns(1)

        topic.publishAt(6000)
        awaitRuns(2)

        timer.stop()
    }

    @Test
    fun waitsForTheInitialDelayBeforeTheFirstAction() = runBlocking {
        val timer = timer()

        timer.interval(1000, 3000)
        val topic = topics.awaitTopic()

        topic.publishAt(2999)
        assertNoMoreRuns(0)

        topic.publishAt(3000)
        awaitRuns(1)

        // Subsequent runs use the period rather than the initial delay
        topic.publishAt(4000)
        awaitRuns(2)

        timer.stop()
    }

    @Test
    fun onceRunsTheActionASingleTimeAfterTheDelay() = runBlocking {
        val timer = timer()

        timer.once(2000)
        val topic = topics.awaitTopic()
        assertEquals(listOf(2000L), topics.periods)

        topic.publishAt(1999)
        assertNoMoreRuns(0)

        topic.publishAt(2000)
        awaitRuns(1)

        waitFor("timer kept running") { !timer.isRunning() }
        assertFalse(topic.isSubscribed)

        time.elapsedMillis = 10000
        topic.publish()
        assertNoMoreRuns(1)
    }

    @Test
    fun takesTheNextPublishWhenThereIsNoTolerance() = runBlocking {
        val timer = TopicTimer(topics, time) { runs.incrementAndGet() }

        timer.interval(1000, 1000)
        val topic = topics.awaitTopic()

        // The topic paces itself, so any publish is close enough to the deadline
        topic.publishAt(1)
        awaitRuns(1)

        timer.stop()
    }

    @Test
    fun acceptsPublishesWithinTheTolerance() = runBlocking {
        val timer = timer(toleranceMillis = 100)

        timer.interval(1000, 1000)
        val topic = topics.awaitTopic()

        topic.publishAt(899)
        assertNoMoreRuns(0)

        topic.publishAt(900)
        awaitRuns(1)

        timer.stop()
    }

    @Test
    fun aRunWhichIsAlreadyDueDoesNotWaitForATopic() = runBlocking {
        val timer = timer()

        // IntervalService uses once(0) to run work a scheduled worker has already woken it for
        timer.once(0)

        awaitRuns(1)
        assertTrue(topics.periods.isEmpty())
        waitFor("timer kept running") { !timer.isRunning() }
    }

    @Test
    fun restartingCreatesANewTopic() = runBlocking {
        val timer = timer()

        timer.interval(1000, 1000)
        val oldTopic = topics.awaitTopic()

        timer.interval(2000, 2000)
        val topic = topics.awaitTopic()

        assertEquals(listOf(1000L, 2000L), topics.periods)
        waitFor("old topic was left subscribed") { !oldTopic.isSubscribed }

        // A publish from the previous run is ignored
        time.elapsedMillis = 2000
        oldTopic.publish()
        assertNoMoreRuns(0)

        topic.publishAt(2000)
        awaitRuns(1)

        timer.stop()
    }

    @Test
    fun publishesAfterStopAreIgnored() = runBlocking {
        val timer = timer()

        timer.interval(1000, 1000)
        val topic = topics.awaitTopic()
        timer.stop()

        waitFor("timer did not unsubscribe") { !topic.isSubscribed }
        time.elapsedMillis = 1000
        topic.publish()
        assertNoMoreRuns(0)
    }

    @Test
    fun stoppingDuringTheActionCancelsIt() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val timer = timer {
            started.complete(Unit)
            delay(10000)
            runs.incrementAndGet()
        }

        timer.interval(1000)
        awaitStart(started)
        timer.stop()

        assertNoMoreRuns(0)
        assertFalse(timer.isRunning())
    }

    @Test
    fun usesASingleTopicWhenOneIsProvided() = runBlocking {
        val topic = TestTopic()
        val timer = TopicTimer(topic, time, 0) { runs.incrementAndGet() }

        timer.interval(1000, 1000)
        topic.publishAt(1000)
        awaitRuns(1)

        topic.publishAt(2000)
        awaitRuns(2)

        timer.stop()
        waitFor("timer did not unsubscribe") { !topic.isSubscribed }
    }

    /**
     * Advances the clock and publishes once the timer is waiting on the topic. The timer captures
     * its deadline before it subscribes, so moving the clock any earlier would race with it.
     */
    private suspend fun TestTopic.publishAt(elapsedMillis: Long) {
        waitFor("timer did not subscribe") { isSubscribed }
        time.elapsedMillis = elapsedMillis
        publish()
    }

    private suspend fun awaitStart(started: CompletableDeferred<Unit>) {
        waitFor("action did not start") { started.isCompleted }
    }

    private suspend fun awaitRuns(expected: Int) {
        waitFor("expected $expected runs, but there were ${runs.get()}") { runs.get() >= expected }
        assertEquals(expected, runs.get())
    }

    /** Gives the timer a chance to run the action before checking that it didn't. */
    private suspend fun assertNoMoreRuns(expected: Int) {
        delay(50)
        assertEquals(expected, runs.get())
    }

    private class FakeTimeProvider(@Volatile var elapsedMillis: Long = 0L) : TimeProvider {
        override fun elapsedRealtime() = elapsedMillis
        override fun currentTimeMillis() = elapsedMillis
    }

    private class TestTopic : BaseTopic() {
        @Volatile
        private var subscriberCount = 0

        override val topic = Topic(
            onSubscriberAdded = { count, _ -> subscriberCount = count },
            onSubscriberRemoved = { count, _ -> subscriberCount = count }
        )

        val isSubscribed: Boolean
            get() = subscriberCount > 0

        fun publish() = topic.publish()
    }

    private class TestTopicProvider : (Long) -> ITopic {
        val periods = CopyOnWriteArrayList<Long>()

        @Volatile
        private var current: TestTopic? = null

        override fun invoke(periodMillis: Long): ITopic {
            val topic = TestTopic()
            current = topic
            periods.add(periodMillis)
            return topic
        }

        /** Waits for the timer to create a topic and subscribe to it. */
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
