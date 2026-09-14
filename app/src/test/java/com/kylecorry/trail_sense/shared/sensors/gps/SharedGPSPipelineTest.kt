package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.core.time.TimeProvider
import com.kylecorry.luna.time.ITimer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.settings.migrations.InMemoryPreferences
import com.kylecorry.trail_sense.shared.GeoidService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.gps.modules.BadReadingFilterGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.CacheGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.KalmanGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.MeanSeaLevelGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.SameFixGPSModule
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
    // The device booted at the epoch
    private val timeProvider = object : TimeProvider {
        override fun elapsedRealtime() = 1_000_000_000L
        override fun currentTimeMillis() = 1_000_000_000L
    }
    private val prefs = mock<IGPSPreferences> {
        on { smoothing }.thenReturn(100)
    }
    private val shared = SharedGPSPipeline(mock()) {
        GPSPipeline(
            listOf(
                BadReadingFilterGPSModule(mock(), timeProvider),
                KalmanGPSModule(prefs, mock()),
                cacheModule()
            )
        )
    }

    private fun cacheModule() = CacheGPSModule(cache, timeProvider) { 1 }

    private fun reading(seconds: Long, longitude: Double = 1.0) = ModularGPSData(
        location = Coordinate(1.0, longitude), eventTime = Instant.EPOCH.plusSeconds(seconds),
        eventTimeElapsedNanos = seconds * 1_000_000_000,
        horizontalAccuracy = 10f, hasValidReading = true
    )

    private class Consumer(shared: SharedGPSPipeline) {
        var notifications = 0
        val consumer = GPSPipelineConsumer(shared) { notifications++ }
    }

    @Test
    fun startupConsidersOnlyRecentNewerFixes() = runBlocking<Unit> {
        val cases = listOf(
            999_994L to 999_994L,
            999_994L to 999_995L,
            999_994L to 999_999L,
            999_994L to 1_000_000L,
            999_994L to 1_000_001L,
            999_999L to 999_998L,
            999_999L to 999_999L
        )
        for ((previous, seconds) in cases) {
            val inputs = mutableListOf<Long>()
            val pipeline = SharedGPSPipeline(mock(), timeProvider) {
                GPSPipeline(listOf(object : GPSModule {
                    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
                        inputs.add(newData.eventTimeElapsedNanos)
                        return true
                    }
                }))
            }
            val first = Consumer(pipeline).consumer
            first.start()
            first.update(reading(previous))
            inputs.clear()
            val second = Consumer(pipeline).consumer
            val accepted = seconds in 999_995L..1_000_000L && seconds > previous
            assertEquals(accepted, second.start(reading(seconds)), "Startup fix at $seconds")
            assertEquals(if (accepted) listOf(reading(seconds).eventTimeElapsedNanos) else emptyList<Long>(), inputs)
            val expected = reading(if (accepted) seconds else previous)
            assertEquals(expected.eventTimeElapsedNanos, second.reading.eventTimeElapsedNanos)
            assertFalse(second.update(expected))
            second.stop()
            first.stop()
        }
    }

    @Test
    fun startupWithoutLocationAcceptsBaseReadingButOnlyNotifiesForRecentNewerFixes() = runBlocking<Unit> {
        for (seconds in listOf(0L, 999_994L, 999_995L, 1_000_000L)) {
            val pipeline = SharedGPSPipeline(mock(), timeProvider) { GPSPipeline(emptyList()) }
            val consumer = Consumer(pipeline).consumer
            val initial = reading(seconds)
            assertEquals(seconds >= 999_995L, consumer.start(initial), "Startup fix at $seconds")
            assertEquals(initial.location, consumer.reading.location)
            assertEquals(initial.eventTimeElapsedNanos, consumer.reading.eventTimeElapsedNanos)
            assertFalse(consumer.update(initial))
            consumer.stop()
        }
    }

    @Test
    fun startupFixStillPassesThroughFilters() = runBlocking<Unit> {
        val pipeline = SharedGPSPipeline(mock(), timeProvider) {
            GPSPipeline(listOf(object : GPSModule {
                override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData) = false
            }))
        }
        val consumer = Consumer(pipeline).consumer
        assertFalse(consumer.start(reading(999_994)))
        assertEquals(Coordinate.zero, consumer.reading.location)
        consumer.stop()
        assertFalse(consumer.start(reading(999_999)))
        assertEquals(Coordinate.zero, consumer.reading.location)
        consumer.stop()
    }

    @Test
    fun suspendingModuleSerializesConsumers() = runBlocking<Unit> {
        val entered = kotlinx.coroutines.CompletableDeferred<Unit>()
        val resume = kotlinx.coroutines.CompletableDeferred<Unit>()
        val previousTimes = mutableListOf<Instant>()
        val pipeline = SharedGPSPipeline(mock()) {
            GPSPipeline(listOf(object : GPSModule {
                override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
                    previousTimes.add(previousData.eventTime)
                    if (newData.eventTime == reading(1).eventTime) {
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
        assertEquals(listOf(Instant.EPOCH, reading(1).eventTime), previousTimes)
        assertEquals(reading(2).eventTime, pipeline.reading.eventTime)
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
        // A delayed callback is rejected rather than rewinding the shared state.
        assertFalse(slow.update(reading(2, 1.001)))
        assertEquals(Instant.EPOCH.plusSeconds(3), slow.reading.eventTime)
        assertEquals(fast.reading.location, slow.reading.location)
        // The slower subscription still delivers the shared fix once it catches up.
        assertTrue(slow.update(reading(3, 1.002)))
    }

    @Test
    fun lateConsumerIsNotDeliveredTheFixItStartedWith() = runBlocking<Unit> {
        val first = Consumer(shared).consumer
        first.start()
        assertTrue(first.update(reading(1)))
        val second = Consumer(shared).consumer
        assertFalse(second.start())
        assertFalse(second.update(reading(1)))
        second.stop()
        first.stop()
    }

    @Test
    fun timeoutQueuedDuringUpdateCannotExpireTheNewFix() = runBlocking<Unit> {
        val entered = kotlinx.coroutines.CompletableDeferred<Unit>()
        val resume = kotlinx.coroutines.CompletableDeferred<Unit>()
        lateinit var fireTimeout: suspend () -> Unit
        val pipeline = SharedGPSPipeline(mock()) { notifyTimeout ->
            GPSPipeline(listOf(
                object : GPSModule {
                    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
                        if (newData.eventTime == reading(2).eventTime) {
                            entered.complete(Unit)
                            resume.await()
                        }
                        return true
                    }
                },
                TimeoutGPSModule(notifyTimeout, mock(), { fireTimeout = it; mock() }, timeProvider)
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
    fun timeoutRerunsTheRejectedLastInputThroughThePipeline() = runBlocking<Unit> {
        lateinit var fireTimeout: suspend () -> Unit
        val timer = mock<ITimer>()
        val geoid = object : GeoidService {
            override suspend fun getGeoid(location: Coordinate) = 25f
            override fun isSameGeoid(location1: Coordinate, location2: Coordinate) = true
        }
        val pipeline = SharedGPSPipeline(mock()) { notifyTimeout ->
            GPSPipeline(listOf(
                SameFixGPSModule(),
                MeanSeaLevelGPSModule(geoid),
                object : GPSModule {
                    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData) =
                        newData.id == previousData.id || previousData.isTimedOut ||
                            newData.eventTime == reading(1).eventTime
                },
                TimeoutGPSModule(notifyTimeout, mock(), { fireTimeout = it; timer }, timeProvider)
            ))
        }
        val consumer = Consumer(pipeline)
        consumer.consumer.start()
        consumer.consumer.update(reading(1).apply { altitude = 100f })
        assertFalse(consumer.consumer.update(reading(2, 2.0).apply { altitude = 200f }))

        fireTimeout()
        assertEquals(reading(2, 2.0).location, pipeline.reading.location)
        assertEquals(175f, pipeline.reading.altitude)
        assertFalse(pipeline.reading.isTimedOut)
        assertEquals(1, consumer.notifications)
        // The accepted fix was already delivered
        assertFalse(consumer.consumer.update(reading(2, 2.0).apply { altitude = 200f }))
        // Stopping the fired timer would cancel the timeout callback
        verify(timer, times(3)).once(SensorService.GPS_READ_TIMEOUT)
        verify(timer, times(1)).stop()

        // Without a newer input, the rerun is the same fix and the reading stays timed out
        fireTimeout()
        assertEquals(175f, pipeline.reading.altitude)
        assertTrue(pipeline.reading.isTimedOut)
        assertEquals(2, consumer.notifications)

        // Inputs are let through until a new fix is accepted
        assertTrue(consumer.consumer.update(reading(3, 3.0).apply { altitude = 300f }))
        assertEquals(275f, pipeline.reading.altitude)
        assertFalse(pipeline.reading.isTimedOut)
        consumer.consumer.stop()
    }

    @Test
    fun timeoutDoesNotRerunAnInputFromBeforeTheRestart() = runBlocking<Unit> {
        lateinit var fireTimeout: suspend () -> Unit
        val pipeline = SharedGPSPipeline(mock()) { notifyTimeout ->
            GPSPipeline(listOf(
                object : GPSModule {
                    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData) =
                        previousData.isTimedOut || newData.eventTime == reading(1).eventTime
                },
                TimeoutGPSModule(notifyTimeout, mock(), { fireTimeout = it; mock() }, timeProvider)
            ))
        }
        val first = Consumer(pipeline)
        first.consumer.start()
        first.consumer.update(reading(1))
        assertFalse(first.consumer.update(reading(2, 2.0)))
        first.consumer.stop()

        val second = Consumer(pipeline)
        second.consumer.start()
        fireTimeout()
        assertEquals(reading(1).location, pipeline.reading.location)
        assertTrue(pipeline.reading.isTimedOut)
        second.consumer.stop()
    }

    @Test
    fun timeoutFromClearedPipelineCannotExpireItsReplacement()= runBlocking<Unit> {
        lateinit var fireTimeout: suspend () -> Unit
        val pipeline = SharedGPSPipeline(mock()) { notifyTimeout ->
            GPSPipeline(listOf(TimeoutGPSModule(notifyTimeout, mock(), { fireTimeout = it; mock() }, timeProvider)))
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
            cacheModule().restore(restored)
            assertEquals(continuous.reading.kalmanState, restored.kalmanState)
        }
    }

    @Test
    fun startingDoesNotDeliverTheRestoredCache() = runBlocking<Unit> {
        cacheModule().update(ModularGPSData(), reading(10))
        val pipeline = SharedGPSPipeline(mock()) { GPSPipeline(listOf(cacheModule())) }
        val consumer = Consumer(pipeline).consumer
        // The cache is available to the consumer, it just isn't delivered as if it were a new fix
        assertFalse(consumer.start())
        assertEquals(reading(10).location, consumer.reading.location)
        assertFalse(consumer.update(reading(10)))
        consumer.stop()
        assertFalse(consumer.start())
        consumer.stop()
    }

    @Test
    fun restartedConsumerIsDeliveredTheFixItMissed() = runBlocking<Unit> {
        val first = Consumer(shared).consumer
        val second = Consumer(shared).consumer
        assertFalse(first.start())
        second.start()
        assertTrue(second.update(reading(1)))
        second.stop()
        assertTrue(first.update(reading(2, 1.001)))
        assertTrue(second.start())
        assertFalse(second.update(reading(2, 1.001)))
        second.stop()
        first.stop()
    }

    @Test
    fun cachedFixIsRestoredBeforeAnyConsumerStarts() = runBlocking<Unit> {
        cacheModule().update(ModularGPSData(), reading(10))
        val pipeline = SharedGPSPipeline(mock()) { GPSPipeline(listOf(cacheModule())) }
        assertEquals(reading(10).location, pipeline.reading.location)
        assertEquals(reading(10).eventTime, pipeline.reading.eventTime)
    }

    @Test
    fun rejectedUpdateKeepsRestoredCacheWithoutDelivering() = runBlocking<Unit> {
        cacheModule().update(ModularGPSData(), reading(10))
        val pipeline = SharedGPSPipeline(mock()) {
            GPSPipeline(listOf(
                object : GPSModule {
                    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData) = false
                },
                cacheModule()
            ))
        }
        val restored = pipeline.reading
        assertEquals(reading(10).location, restored.location)
        assertEquals(reading(10).eventTime, restored.eventTime)
        // A one-shot read must keep waiting for a fix instead of finishing with the cache.
        val consumer = Consumer(pipeline).consumer
        assertFalse(consumer.update(reading(11, 2.0)))
        assertNull(pipeline.update(reading(12, 3.0)))
        assertFalse(consumer.update(reading(12, 3.0)))
        assertSame(restored, pipeline.reading)
    }

    @Test
    fun olderCallbacksCannotRewindState() = runBlocking<Unit> {
        shared.update(reading(10))
        val snapshot = shared.reading
        shared.update(reading(9, 2.0))
        assertSame(snapshot, shared.reading)
        shared.update(reading(11, 1.001))
        assertEquals(reading(10).location, snapshot.location)
        assertEquals(reading(10).eventTime, snapshot.eventTime)
    }

    @Test
    fun anyConsumerPostponesSharedTimeoutAndAllActiveConsumersAreNotified() = runBlocking<Unit> {
        val timer = mock<ITimer>()
        lateinit var fireTimeout: suspend () -> Unit
        val pipeline = SharedGPSPipeline(mock()) { notifyTimeout ->
            GPSPipeline(listOf(
                TimeoutGPSModule(
                    notifyTimeout, logger = mock(),
                    timerFactory = { fireTimeout = it; timer },
                    timeProvider = timeProvider
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
        // Fired timers are not stopped
        verify(timer, times(2)).stop()
        fireTimeout()
        assertEquals(2, fast.notifications)
    }

    @Test
    fun restartingAfterTheLastConsumerStopsClearsTheTimeout() = runBlocking<Unit> {
        lateinit var fireTimeout: suspend () -> Unit
        val pipeline = SharedGPSPipeline(mock()) { notifyTimeout ->
            GPSPipeline(listOf(TimeoutGPSModule(notifyTimeout, mock(), { fireTimeout = it; mock() }, timeProvider)))
        }
        val first = Consumer(pipeline)
        first.consumer.start()
        first.consumer.update(reading(1))
        fireTimeout()
        assertTrue(first.consumer.reading.isTimedOut)
        first.consumer.stop()

        val second = Consumer(pipeline)
        second.consumer.start()
        assertFalse(second.consumer.reading.isTimedOut)
        assertFalse(pipeline.isTimedOut)
        second.consumer.stop()
    }

    @Test
    fun modulesRunUntilLastConsumerStopsAndStateSurvivesRestart()= runBlocking<Unit> {
        var starts = 0
        var stops = 0
        val lifecycle = object : GPSModule {
            override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData) = true
            override suspend fun start(data: ModularGPSData) { starts++ }
            override suspend fun stop(data: ModularGPSData) { stops++ }
        }
        val pipeline = SharedGPSPipeline(mock()) { GPSPipeline(listOf(lifecycle)) }
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
        val pipeline = SharedGPSPipeline(mock()) {
            GPSPipeline(listOf(KalmanGPSModule(prefs, mock()), cacheModule()))
        }
        pipeline.update(reading(1))
        pipeline.update(reading(2, 1.001))
        assertTrue(pipeline.reading.location.longitude < 1.001)
        whenever(prefs.smoothing).thenReturn(0)
        pipeline.update(reading(3, 1.002))
        assertEquals(reading(3, 1.002).location, pipeline.reading.location)
        val restored = ModularGPSData()
        cacheModule().restore(restored)
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
            assertEquals(reading(40).eventTime, shared.reading.eventTime)
            val restored = ModularGPSData()
            cacheModule().restore(restored)
            assertEquals(shared.reading.eventTime, restored.eventTime)
            assertEquals(shared.reading.location, restored.location)
            assertNotNull(restored.kalmanState)
            assertTrue(restored.kalmanState!!.state.all { it.isFinite() })
        } finally {
            executor.shutdownNow()
        }
    }
}
