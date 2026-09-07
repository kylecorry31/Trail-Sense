package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.luna.time.ITimer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.settings.migrations.InMemoryPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.gps.modules.CacheGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.KalmanGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.TimeoutGPSModule
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SharedGPSPipelineTest {
    private val cache = InMemoryPreferences()
    private val prefs = mock<IGPSPreferences> {
        on { smoothing }.thenReturn(100)
    }
    private val shared = SharedGPSPipeline {
        GPSPipeline(listOf(KalmanGPSModule(prefs, mock()), CacheGPSModule(cache)))
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
    fun suspendingModuleSerializesConsumers() = runBlocking<Unit> {
        val entered = kotlinx.coroutines.CompletableDeferred<Unit>()
        val resume = kotlinx.coroutines.CompletableDeferred<Unit>()
        val previousTimes = mutableListOf<Instant>()
        val pipeline = SharedGPSPipeline {
            GPSPipeline(listOf(object : GPSModule {
                override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
                    previousTimes.add(previousData.time)
                    if (newData.time == reading(1).time) {
                        entered.complete(Unit)
                        resume.await()
                    }
                    return true
                }
            }))
        }
        val first = async { pipeline.update(reading(1)) }
        entered.await()
        val second = async { pipeline.update(reading(2)) }
        yield()
        assertFalse(second.isCompleted)
        resume.complete(Unit)
        first.await()
        second.await()
        assertEquals(listOf(Instant.EPOCH, reading(1).time), previousTimes)
        assertEquals(reading(2).time, pipeline.reading.time)
    }

    @Test
    fun consumersShareEstimatesButDeliverFixesIndependently() = runBlocking<Unit> {
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
    fun lateConsumerDeliversExistingFixOnlyOnce() = runBlocking<Unit> {
        val first = Consumer(shared).consumer
        first.start()
        assertTrue(first.update(reading(1)))
        val second = Consumer(shared).consumer
        second.start()
        assertTrue(second.update(reading(1)))
        assertFalse(second.update(reading(1)))
        second.stop()
        first.stop()
    }

    @Test
    fun timeoutQueuedDuringUpdateCannotExpireTheNewFix() = runBlocking<Unit> {
        val entered = kotlinx.coroutines.CompletableDeferred<Unit>()
        val resume = kotlinx.coroutines.CompletableDeferred<Unit>()
        lateinit var fireTimeout: suspend () -> Unit
        val pipeline = SharedGPSPipeline { notifyTimeout ->
            GPSPipeline(listOf(
                object : GPSModule {
                    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
                        if (newData.time == reading(2).time) {
                            entered.complete(Unit)
                            resume.await()
                        }
                        return true
                    }
                },
                TimeoutGPSModule(notifyTimeout, mock(), { fireTimeout = it; mock() })
            ))
        }
        val first = Consumer(pipeline)
        val second = Consumer(pipeline)
        first.consumer.start()
        second.consumer.start()
        first.consumer.update(reading(1))
        val oldTimeout = fireTimeout
        val update = async { second.consumer.update(reading(2)) }
        entered.await()
        val timeout = async { oldTimeout() }
        yield()
        assertFalse(timeout.isCompleted)
        assertFalse(pipeline.reading.isTimedOut)
        resume.complete(Unit)
        update.await()
        timeout.await()
        assertFalse(pipeline.reading.isTimedOut)
        assertEquals(0, first.notifications)
        assertEquals(0, second.notifications)
        oldTimeout()
        assertFalse(pipeline.reading.isTimedOut)
        fireTimeout()
        assertTrue(pipeline.reading.isTimedOut)
        assertEquals(1, first.notifications)
        assertEquals(1, second.notifications)
        first.consumer.stop()
        second.consumer.stop()
    }

    @Test
    fun timeoutFromClearedPipelineCannotExpireItsReplacement() = runBlocking<Unit> {
        lateinit var fireTimeout: suspend () -> Unit
        val pipeline = SharedGPSPipeline { notifyTimeout ->
            GPSPipeline(listOf(TimeoutGPSModule(notifyTimeout, mock(), { fireTimeout = it; mock() })))
        }
        val consumer = Consumer(pipeline)
        consumer.consumer.start()
        consumer.consumer.update(reading(1))
        val oldTimeout = fireTimeout
        pipeline.clearCache {}
        consumer.consumer.update(reading(2))
        oldTimeout()
        assertFalse(pipeline.reading.isTimedOut)
        assertEquals(0, consumer.notifications)
        fireTimeout()
        assertTrue(pipeline.reading.isTimedOut)
        assertEquals(1, consumer.notifications)
        consumer.consumer.stop()
    }

    @Test
    fun duplicateCallbacksDoNotApplyKalmanCorrectionTwice() = runBlocking<Unit> {
        val continuous = GPSPipeline(listOf(KalmanGPSModule(prefs, mock())))
        for (second in 1L..10L) {
            val source = reading(second, 1.0 + second * 0.0001)
            continuous.update(source)
            shared.update(source)
            repeat(5) { shared.update(source) }
            assertEquals(continuous.reading.location, shared.reading.location)
            val restored = ModularGPSData()
            CacheGPSModule(cache).restore(restored)
            assertEquals(continuous.reading.kalmanState, restored.kalmanState)
        }
    }

    @Test
    fun rejectedUpdatePublishesLazilyRestoredCacheWithoutDelivering() = runBlocking<Unit> {
        CacheGPSModule(cache).update(ModularGPSData(), reading(10))
        val pipeline = SharedGPSPipeline {
            GPSPipeline(listOf(
                object : GPSModule {
                    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData) = false
                },
                CacheGPSModule(cache)
            ))
        }
        assertEquals(Coordinate.zero, pipeline.reading.location)
        // A one-shot read must keep waiting for a fix instead of finishing with the cache.
        val consumer = Consumer(pipeline).consumer
        assertFalse(consumer.update(reading(11, 2.0)))
        val restored = pipeline.reading
        assertEquals(reading(10).location, restored.location)
        assertEquals(reading(10).time, restored.time)
        assertNull(pipeline.update(reading(12, 3.0)))
        assertFalse(consumer.update(reading(12, 3.0)))
        assertSame(restored, pipeline.reading)
    }

    @Test
    fun recoversWhenPreviousFixTimeIsInTheFuture() = runBlocking<Unit> {
        val pipeline = SharedGPSPipeline { GPSPipeline(emptyList()) }
        val future = reading(1).apply { time = Instant.now().plusSeconds(3600) }
        pipeline.update(future)
        val recovered = pipeline.update(reading(2, 2.0))!!
        assertEquals(reading(2).time, recovered.time)
        assertEquals(reading(2, 2.0).location, recovered.location)
    }

    @Test
    fun olderCallbacksCannotRewindStateEvenWithoutRejectionModule() = runBlocking<Unit> {
        shared.update(reading(10))
        val snapshot = shared.reading
        shared.update(reading(9, 2.0))
        assertSame(snapshot, shared.reading)
        shared.update(reading(11, 1.001))
        assertEquals(reading(10).location, snapshot.location)
        assertEquals(reading(10).time, snapshot.time)
    }

    @Test
    fun anyConsumerPostponesSharedTimeoutAndAllActiveConsumersAreNotified() = runBlocking<Unit> {
        val timer = mock<ITimer>()
        lateinit var fireTimeout: suspend () -> Unit
        val pipeline = SharedGPSPipeline { notifyTimeout ->
            GPSPipeline(listOf(
                TimeoutGPSModule(
                notifyTimeout, logger = mock(),
                timerFactory = { fireTimeout = it; timer }
            )))
        }
        val fast = Consumer(pipeline)
        val slow = Consumer(pipeline)
        fast.consumer.start()
        slow.consumer.start()
        verify(timer).once(SensorService.GPS_READ_TIMEOUT)
        fast.consumer.update(reading(1))
        fast.consumer.update(reading(2))
        verify(timer, times(3)).once(SensorService.GPS_READ_TIMEOUT)
        assertFalse(slow.consumer.reading.isTimedOut)

        fireTimeout()
        assertTrue(fast.consumer.reading.isTimedOut)
        assertTrue(slow.consumer.reading.isTimedOut)
        assertEquals(1, fast.notifications)
        assertEquals(1, slow.notifications)
        slow.consumer.update(reading(2))
        assertTrue(slow.consumer.reading.isTimedOut)
        verify(timer, times(3)).once(SensorService.GPS_READ_TIMEOUT)

        fast.consumer.update(reading(3))
        assertFalse(fast.consumer.reading.isTimedOut)
        assertFalse(slow.consumer.reading.isTimedOut)
        verify(timer, times(4)).once(SensorService.GPS_READ_TIMEOUT)
        slow.consumer.stop()
        fireTimeout()
        assertEquals(2, fast.notifications)
        assertEquals(1, slow.notifications)
        fast.consumer.stop()
        verify(timer, times(4)).stop()
        fireTimeout()
        assertEquals(2, fast.notifications)
    }

    @Test
    fun modulesRunUntilLastConsumerStopsAndStateSurvivesRestart() = runBlocking<Unit> {
        var starts = 0
        var stops = 0
        val lifecycle = object : GPSModule {
            override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData) = true
            override suspend fun start(data: ModularGPSData) { starts++ }
            override suspend fun stop(data: ModularGPSData) { stops++ }
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
    fun clearingCacheResetsSharedEstimateAndFilter() = runBlocking<Unit> {
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
    fun smoothingPreferenceChangesApplyToExistingPipeline() = runBlocking<Unit> {
        val pipeline = SharedGPSPipeline {
            GPSPipeline(listOf(KalmanGPSModule(prefs, mock()), CacheGPSModule(cache)))
        }
        pipeline.update(reading(1))
        pipeline.update(reading(2, 1.001))
        assertTrue(pipeline.reading.location.longitude < 1.001)
        whenever(prefs.smoothing).thenReturn(0)
        pipeline.update(reading(3, 1.002))
        assertEquals(reading(3, 1.002).location, pipeline.reading.location)
        val restored = ModularGPSData()
        CacheGPSModule(cache).restore(restored)
        assertNull(restored.kalmanState)
        whenever(prefs.smoothing).thenReturn(100)
        pipeline.update(reading(4, 1.003))
        assertTrue(pipeline.reading.location.longitude > 1.002)
        assertTrue(pipeline.reading.location.longitude < 1.003)
    }

    @Test
    fun concurrentSubscriptionsCannotOverwriteNewerFixes() = runBlocking<Unit> {
        val executor = Executors.newFixedThreadPool(4)
        val ready = CountDownLatch(1)
        try {
            val tasks = (1L..40L).map { second ->
                executor.submit {
                    ready.await()
                    kotlinx.coroutines.runBlocking { shared.update(reading(second, 1.0 + second * 0.0001)) }
                }
            }
            ready.countDown()
            tasks.forEach { it.get(5, TimeUnit.SECONDS) }
            assertEquals(reading(40).time, shared.reading.time)
            val restored = ModularGPSData()
            CacheGPSModule(cache).restore(restored)
            assertEquals(shared.reading.time, restored.time)
            assertEquals(shared.reading.location, restored.location)
            assertNotNull(restored.kalmanState)
            assertTrue(restored.kalmanState!!.state.all { it.isFinite() })
        } finally {
            executor.shutdownNow()
        }
    }
}
