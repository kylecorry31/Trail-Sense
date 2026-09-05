package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.luna.time.ITimer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.settings.migrations.InMemoryPreferences
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SharedGPSPipelineTest {
    private val cache = InMemoryPreferences()
    private val shared = SharedGPSPipeline {
        GPSPipeline(listOf(KalmanGPSModule(mock()), CacheGPSModule(cache)))
    }

    private fun reading(seconds: Long, longitude: Double = 1.0) = ModularGPSData(
        location = Coordinate(1.0, longitude), time = Instant.EPOCH.plusSeconds(seconds),
        horizontalAccuracy = 10f, hasValidReading = true
    )

    private class Consumer(shared: SharedGPSPipeline) {
        var notifications = 0
        val consumer = GPSPipelineConsumer(shared) { notifications++ }
    }

    @Test
    fun consumersShareEstimatesButDeliverFixesIndependently() {
        val fast = Consumer(shared).consumer
        val slow = Consumer(shared).consumer
        assertTrue(fast.update(reading(1)))
        assertTrue(slow.update(reading(1)))
        assertFalse(fast.update(reading(1)))
        assertFalse(slow.update(reading(1)))

        fast.update(reading(2, 1.001))
        fast.update(reading(3, 1.002))
        assertEquals(fast.reading.location, slow.reading.location)
        // The slower subscription sees the latest result even if its callback is delayed.
        assertTrue(slow.update(reading(2, 1.001)))
        assertEquals(Instant.EPOCH.plusSeconds(3), slow.reading.time)
        assertEquals(fast.reading.location, slow.reading.location)
        assertFalse(slow.update(reading(3, 1.002)))
    }

    @Test
    fun duplicateCallbacksDoNotApplyKalmanCorrectionTwice() {
        val continuous = GPSPipeline(listOf(KalmanGPSModule(mock())))
        for (second in 1L..10L) {
            val source = reading(second, 1.0 + second * 0.0001)
            continuous.update(source)
            shared.update(source)
            repeat(5) { shared.update(source) }
            assertEquals(continuous.reading.location, shared.reading.location)
            val restored = ModularGPSData()
            CacheGPSModule(cache).restore(restored)
            assertEquals(continuous.reading.kalmanVariance, restored.kalmanVariance)
        }
    }

    @Test
    fun olderCallbacksCannotRewindStateEvenWithoutRejectionModule() {
        shared.update(reading(10))
        val snapshot = shared.reading
        shared.update(reading(9, 2.0))
        assertSame(snapshot, shared.reading)
        shared.update(reading(11, 1.001))
        assertEquals(reading(10).location, snapshot.location)
        assertEquals(reading(10).time, snapshot.time)
    }

    @Test
    fun anyConsumerPostponesSharedTimeoutAndAllActiveConsumersAreNotified() {
        val timer = mock<ITimer>()
        lateinit var fireTimeout: () -> Unit
        val pipeline = SharedGPSPipeline { notifyTimeout ->
            GPSPipeline(listOf(TimeoutGPSModule(
                notifyTimeout, logger = mock(),
                timerFactory = { fireTimeout = it; timer }
            )))
        }
        val fast = Consumer(pipeline)
        val slow = Consumer(pipeline)
        fast.consumer.start()
        slow.consumer.start()
        verify(timer).once(Duration.ofSeconds(10))
        fast.consumer.update(reading(1))
        fast.consumer.update(reading(2))
        verify(timer, times(3)).once(Duration.ofSeconds(10))
        assertFalse(slow.consumer.reading.isTimedOut)

        fireTimeout()
        assertTrue(fast.consumer.reading.isTimedOut)
        assertTrue(slow.consumer.reading.isTimedOut)
        assertEquals(1, fast.notifications)
        assertEquals(1, slow.notifications)
        slow.consumer.update(reading(2))
        assertTrue(slow.consumer.reading.isTimedOut)
        verify(timer, times(3)).once(Duration.ofSeconds(10))

        fast.consumer.update(reading(3))
        assertFalse(fast.consumer.reading.isTimedOut)
        assertFalse(slow.consumer.reading.isTimedOut)
        verify(timer, times(4)).once(Duration.ofSeconds(10))
        slow.consumer.stop()
        fireTimeout()
        assertEquals(2, fast.notifications)
        assertEquals(1, slow.notifications)
        fast.consumer.stop()
        verify(timer).stop()
        fireTimeout()
        assertEquals(2, fast.notifications)
    }

    @Test
    fun modulesRunUntilLastConsumerStopsAndStateSurvivesRestart() {
        var starts = 0
        var stops = 0
        val lifecycle = object : GPSModule {
            override fun update(previousData: ModularGPSData, newData: ModularGPSData) = true
            override fun start(data: ModularGPSData) { starts++ }
            override fun stop(data: ModularGPSData) { stops++ }
        }
        val pipeline = SharedGPSPipeline { GPSPipeline(listOf(lifecycle)) }
        val first = Any()
        val second = Any()
        pipeline.start(first)
        pipeline.start(first)
        pipeline.start(second)
        pipeline.update(reading(1))
        assertEquals(1, starts)
        pipeline.stop(first)
        pipeline.stop(first)
        assertEquals(0, stops)
        pipeline.stop(second)
        assertEquals(1, stops)
        pipeline.start(second)
        assertEquals(2, starts)
        assertEquals(reading(1).location, pipeline.reading.location)
        pipeline.stop(second)
    }

    @Test
    fun clearingCacheResetsSharedEstimateAndFilter() {
        val consumer = Consumer(shared).consumer
        consumer.start()
        consumer.update(reading(1))
        consumer.update(reading(2, 1.001))
        shared.clearCache { cache.clear() }
        assertEquals(Coordinate.zero, consumer.reading.location)
        assertTrue(consumer.update(reading(3, 1.01)))
        assertEquals(reading(3, 1.01).location, consumer.reading.location)
        consumer.stop()
    }

    @Test
    fun smoothingPreferenceChangesApplyToExistingPipeline() {
        var enabled = true
        val pipeline = SharedGPSPipeline {
            GPSPipeline(listOf(KalmanGPSModule(mock(), enabled = { enabled }), CacheGPSModule(cache)))
        }
        pipeline.update(reading(1))
        pipeline.update(reading(2, 1.001))
        assertTrue(pipeline.reading.location.longitude < 1.001)
        enabled = false
        pipeline.update(reading(3, 1.002))
        assertEquals(reading(3, 1.002).location, pipeline.reading.location)
        val restored = ModularGPSData()
        CacheGPSModule(cache).restore(restored)
        assertNull(restored.kalmanVariance)
        enabled = true
        pipeline.update(reading(4, 1.003))
        assertTrue(pipeline.reading.location.longitude > 1.002)
        assertTrue(pipeline.reading.location.longitude < 1.003)
    }

    @Test
    fun concurrentSubscriptionsCannotOverwriteNewerFixes() {
        val executor = Executors.newFixedThreadPool(4)
        val ready = CountDownLatch(1)
        try {
            val tasks = (1L..40L).map { second ->
                executor.submit {
                    ready.await()
                    shared.update(reading(second, 1.0 + second * 0.0001))
                }
            }
            ready.countDown()
            tasks.forEach { it.get(5, TimeUnit.SECONDS) }
            assertEquals(reading(40).time, shared.reading.time)
            val restored = ModularGPSData()
            CacheGPSModule(cache).restore(restored)
            assertEquals(shared.reading.time, restored.time)
            assertEquals(shared.reading.location, restored.location)
            assertTrue(restored.kalmanVariance!!.isFinite())
        } finally {
            executor.shutdownNow()
        }
    }
}
