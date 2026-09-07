package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.settings.migrations.InMemoryPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.TimeProvider
import com.kylecorry.trail_sense.shared.sensors.gps.modules.AccuracyFilterGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.BadReadingFilterGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.CacheGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.KalmanGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.SatelliteFixFilterGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.modules.TimeoutGPSModule
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GPSPipelineTest {
    private val preferences = InMemoryPreferences()
    private val prefs = mock<IGPSPreferences> {
        on { useFilteredGPS }.thenReturn(true)
        on { rejectInvalidReadings }.thenReturn(true)
    }

    private suspend fun pipeline(vararg modules: GPSModule) =
        GPSPipeline(modules.toList() + CacheGPSModule(preferences)).also { it.reinitialize() }

    private fun reading(seconds: Long, longitude: Double = 1.0) = ModularGPSData(
        location = Coordinate(1.0, longitude),
        time = Instant.EPOCH.plusSeconds(seconds),
        horizontalAccuracy = 10f,
        hasValidReading = true
    )

    private fun module(action: (ModularGPSData, ModularGPSData) -> Boolean) = object : GPSModule {
        override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
            return action(previousData, newData)
        }
    }

    @Test
    fun satelliteFallbackStaysOpenWhileAccuracyWaitsForItsTimeout() = runBlocking<Unit> {
        whenever(prefs.requiresSatelliteCount).thenReturn(true)
        whenever(prefs.accuracyFilter).thenReturn(GPSAccuracyFilter.High)
        var now = 0L
        val clock = object : TimeProvider {
            override fun elapsedRealtime() = now
            override fun currentTimeMillis() = now
        }
        val pipeline = pipeline(
            SatelliteFixFilterGPSModule(prefs, mock(), clock),
            AccuracyFilterGPSModule(prefs, mock(), clock)
        )
        fun candidate(seconds: Long) = reading(seconds).apply {
            satellites = 3
            horizontalAccuracy = 100f
        }
        // The pipeline stops at the first rejection, so the accuracy wait only starts once the
        // satellite fallback opens and lets a reading reach the accuracy module.
        val satelliteWait = 5_000L
        val accuracyWait = GPSAccuracyFilter.High.maxAccuracyWait!!.toMillis()
        pipeline.start()
        assertEquals(GPSUpdateResult.Rejected, pipeline.update(candidate(1)))
        now = satelliteWait
        assertEquals(GPSUpdateResult.Rejected, pipeline.update(candidate(2)))
        now = satelliteWait + accuracyWait - 1
        assertEquals(GPSUpdateResult.Rejected, pipeline.update(candidate(3)))
        now = satelliteWait + accuracyWait
        assertEquals(GPSUpdateResult.NewFixAccepted, pipeline.update(candidate(4)))
        assertEquals(candidate(4).time, pipeline.reading.time)
        assertEquals(GPSUpdateResult.Rejected, pipeline.update(candidate(5)))
        pipeline.stop()
    }

    @Test
    fun processesModulesInOrderAndCachesFinalReadingWithoutChangingSource() = runBlocking<Unit> {
        val pipeline = pipeline(
            module { _, next -> next.altitude += 10f; true },
            module { previous, next ->
                assertEquals(Coordinate.zero, previous.location)
                next.altitude *= 2f
                true
            }
        )
        val source = reading(1).apply { altitude = 5f }
        assertEquals(GPSUpdateResult.NewFixAccepted, pipeline.update(source))
        assertEquals(30f, pipeline.reading.altitude)
        assertEquals(5f, source.altitude)
        assertEquals(30f, pipeline().reading.altitude)
        assertTrue(pipeline.hadValidReading)
    }

    @Test
    fun rejectionPreservesAcceptedReadingAndCacheAndSkipsLaterModules() = runBlocking<Unit> {
        var reject = false
        var laterCalls = 0
        val pipeline = pipeline(
            module { _, next -> next.altitude = 25f; !reject },
            module { _, _ -> laterCalls++; true }
        )
        pipeline.update(reading(1))
        reject = true
        assertEquals(GPSUpdateResult.Rejected, pipeline.update(reading(2, 2.0)))
        assertEquals(1, laterCalls)
        assertEquals(reading(1).location, pipeline.reading.location)
        assertEquals(reading(1).time, pipeline.reading.time)
        assertEquals(reading(1).time, pipeline().reading.time)

        reject = false
        assertEquals(GPSUpdateResult.NewFixAccepted, pipeline.update(reading(3)))
    }

    @Test
    fun sameFixUpdatesSecondaryFieldsAndHasAnExplicitResult() = runBlocking<Unit> {
        val pipeline = pipeline()
        pipeline.update(reading(1))
        val duplicate = reading(1).apply { satellites = 8; altitude = 20f }
        assertEquals(GPSUpdateResult.SameFixUpdated, pipeline.update(duplicate))
        assertEquals(8, pipeline.reading.satellites)
        assertEquals(20f, pipeline.reading.altitude)
        assertEquals(20f, pipeline().reading.altitude)
    }

    @Test
    fun restoresNewerCacheAndResynchronizesKalmanAcrossRestarts() = runBlocking<Unit> {
        val first = pipeline(KalmanGPSModule(prefs, mock()))
        first.start()
        first.update(reading(1))
        first.stop()

        val second = pipeline(KalmanGPSModule(prefs, mock()))
        second.update(reading(2, 1.001))
        second.update(reading(3, 1.002))
        assertTrue(first.reinitialize())
        assertEquals(second.reading.location, first.reading.location)
        assertFalse(first.reinitialize())

        first.start()
        assertEquals(GPSUpdateResult.SameFixUpdated, first.update(reading(3, 1.002)))
        assertEquals(second.reading.location, first.reading.location)
        // Restoring includes the internal covariance, so both filters continue identically.
        val expected = reading(4, 1.003)
        KalmanGPSModule(prefs, mock()).update(first.reading, expected)
        first.update(reading(4, 1.003))
        second.update(reading(4, 1.003))
        assertEquals(expected.location, first.reading.location)
        assertEquals(second.reading.location, first.reading.location)
        assertEquals(second.reading.kalmanState, first.reading.kalmanState)
        assertEquals(second.reading.horizontalAccuracy!!, first.reading.horizontalAccuracy!!, 0.00001f)
    }

    @Test
    fun recreatingPipelineForEveryFixMatchesContinuousFiltering() = runBlocking<Unit> {
        val continuous = GPSPipeline(listOf(KalmanGPSModule(prefs, mock())))
        var seconds = 1L
        for ((index, interval) in listOf(1L, 1L, 1L, 15L, 1L, 900L, 1800L, 1L).withIndex()) {
            seconds += interval
            fun source() = reading(seconds, 1.0 + index * 0.0001).apply {
                rawBearing = 90f
                speed = Speed.from(5f, DistanceUnits.Meters, TimeUnits.Seconds)
                speedAccuracy = 0.3f
                bearingAccuracy = 2f
            }
            val recreated = pipeline(KalmanGPSModule(prefs, mock()))
            continuous.update(source())
            recreated.update(source())
            assertEquals(continuous.reading.location, recreated.reading.location)
            assertEquals(continuous.reading.kalmanState, recreated.reading.kalmanState)
        }
    }

    @Test
    fun timeoutStateSurvivesDuplicateAndRejectedReadingsAndClearsOnNewFix() = runBlocking<Unit> {
        lateinit var pipeline: GPSPipeline
        lateinit var fireTimeout: suspend () -> Unit
        var source = reading(1)
        val notifications = mutableListOf<Boolean>()
        val timeout = TimeoutGPSModule(
            onTimeout = { acceptTimeout ->
                if (acceptTimeout()) {
                    pipeline.reading.isTimedOut = true
                    notifications.add(pipeline.reading.isTimedOut)
                }
            },
            logger = mock(),
            timerFactory = { fireTimeout = it; mock() }
        )
        pipeline = pipeline(module { _, next -> next.hasValidReading }, timeout)
        pipeline.start()
        pipeline.update(source)
        fireTimeout()
        assertTrue(pipeline.reading.isTimedOut)
        assertEquals(listOf(true), notifications)

        source = reading(1).apply { satellites = 8 }
        assertEquals(GPSUpdateResult.SameFixUpdated, pipeline.update(source))
        assertTrue(pipeline.reading.isTimedOut)
        assertEquals(8, pipeline.reading.satellites)

        source = reading(2).apply { hasValidReading = false }
        assertEquals(GPSUpdateResult.Rejected, pipeline.update(source))
        assertTrue(pipeline.reading.isTimedOut)

        source = reading(3)
        assertEquals(GPSUpdateResult.NewFixAccepted, pipeline.update(source))
        assertFalse(pipeline.reading.isTimedOut)
        pipeline.stop()
    }

    @Test
    fun grossJumpDoesNotChangeSmoothingOrCache() = runBlocking<Unit> {
        val pipeline = pipeline(
            BadReadingFilterGPSModule(prefs, mock()),
            KalmanGPSModule(prefs, mock())
        )
        pipeline.update(reading(1))
        assertEquals(GPSUpdateResult.Rejected, pipeline.update(reading(2, 2.0)))
        assertEquals(reading(1).location, pipeline.reading.location)
        assertEquals(reading(1).location, pipeline().reading.location)
        assertEquals(GPSUpdateResult.NewFixAccepted, pipeline.update(reading(3, 1.0001)))
        assertTrue(pipeline.reading.location.longitude < 1.0001)
    }

    @Test
    fun initializesFromCacheAndReportsOnlyNewerReadingsAsChanges() = runBlocking<Unit> {
        val writer = pipeline()
        writer.update(reading(1))
        val reader = pipeline()
        assertEquals(reading(1).time, reader.reading.time)
        assertEquals(reading(1).location, reader.reading.location)
        assertFalse(reader.reinitialize())

        writer.update(reading(2, 2.0))
        assertTrue(reader.reinitialize())
        assertEquals(reading(2, 2.0).location, reader.reading.location)
        assertFalse(reader.reinitialize())
    }

    @Test
    fun emptyCacheDoesNotChangeEmptyReading() = runBlocking<Unit> {
        val pipeline = pipeline()
        assertEquals(Instant.EPOCH, pipeline.reading.time)
        assertEquals(Coordinate.zero, pipeline.reading.location)
        assertFalse(pipeline.reinitialize())
    }

    @Test
    fun initializationRunsEveryModuleEvenAfterAChange() = runBlocking<Unit> {
        val calls = mutableListOf<Int>()
        fun initializer(id: Int) = object : GPSModule {
            override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData) = true
            override suspend fun initialize(data: ModularGPSData): Boolean {
                calls.add(id)
                return true
            }
        }
        val pipeline = pipeline(initializer(1), initializer(2))
        assertEquals(listOf(1, 2), calls)
        calls.clear()
        assertTrue(pipeline.reinitialize())
        assertEquals(listOf(1, 2), calls)
    }

    @Test
    fun forwardsLifecycleWithAcceptedReading() = runBlocking<Unit> {
        val events = mutableListOf<String>()
        val pipeline = pipeline(object : GPSModule {
            override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData) = true

            override suspend fun start(data: ModularGPSData) {
                events.add("start:${data.time.epochSecond}")
            }

            override suspend fun stop(data: ModularGPSData) {
                events.add("stop:${data.time.epochSecond}")
            }
        })
        pipeline.start()
        pipeline.update(reading(1))
        pipeline.stop()
        pipeline.start()
        assertEquals(listOf("start:0", "stop:1", "start:1"), events)
    }
}
