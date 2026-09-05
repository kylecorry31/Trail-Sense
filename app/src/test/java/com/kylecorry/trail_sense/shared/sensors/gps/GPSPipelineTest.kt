package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.settings.migrations.InMemoryPreferences
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.Instant

class GPSPipelineTest {
    private val preferences = InMemoryPreferences()

    private fun pipeline(vararg modules: GPSModule) =
        GPSPipeline(modules.toList() + CacheGPSModule(preferences))

    private fun reading(seconds: Long, longitude: Double = 1.0) = ModularGPSData(
        location = Coordinate(1.0, longitude),
        time = Instant.EPOCH.plusSeconds(seconds),
        horizontalAccuracy = 10f,
        hasValidReading = true
    )

    private fun module(action: (ModularGPSData, ModularGPSData) -> Boolean) = object : GPSModule {
        override fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
            return action(previousData, newData)
        }
    }

    @Test
    fun processesModulesInOrderAndCachesFinalReadingWithoutChangingSource() {
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
    fun rejectionPreservesAcceptedReadingAndCacheAndSkipsLaterModules() {
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
    fun sameFixUpdatesSecondaryFieldsAndHasAnExplicitResult() {
        val pipeline = pipeline()
        pipeline.update(reading(1))
        val duplicate = reading(1).apply { satellites = 8; altitude = 20f }
        assertEquals(GPSUpdateResult.SameFixUpdated, pipeline.update(duplicate))
        assertEquals(8, pipeline.reading.satellites)
        assertEquals(20f, pipeline.reading.altitude)
        assertEquals(20f, pipeline().reading.altitude)
    }

    @Test
    fun restoresNewerCacheAndResynchronizesKalmanAcrossRestarts() {
        val first = pipeline(KalmanGPSModule(mock()))
        first.start()
        first.update(reading(1))
        first.stop()

        val second = pipeline(KalmanGPSModule(mock()))
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
        KalmanGPSModule(mock()).update(first.reading, expected)
        first.update(reading(4, 1.003))
        second.update(reading(4, 1.003))
        assertEquals(expected.location, first.reading.location)
        assertEquals(second.reading.location, first.reading.location)
        assertEquals(second.reading.kalmanVariance, first.reading.kalmanVariance)
        assertEquals(second.reading.horizontalAccuracy!!, first.reading.horizontalAccuracy!!, 0.00001f)
    }

    @Test
    fun recreatingPipelineForEveryFixMatchesContinuousFiltering() {
        val continuous = GPSPipeline(listOf(KalmanGPSModule(mock())))
        var seconds = 1L
        for ((index, interval) in listOf(1L, 1L, 1L, 15L, 1L, 900L, 1800L, 1L).withIndex()) {
            seconds += interval
            fun source() = reading(seconds, 1.0 + index * 0.0001).apply {
                rawBearing = 90f
                speed = Speed.from(5f, DistanceUnits.Meters, TimeUnits.Seconds)
                speedAccuracy = 0.3f
                bearingAccuracy = 2f
            }
            val recreated = pipeline(KalmanGPSModule(mock()))
            continuous.update(source())
            recreated.update(source())
            assertEquals(continuous.reading.location, recreated.reading.location)
            assertEquals(continuous.reading.kalmanVariance, recreated.reading.kalmanVariance)
            assertEquals(continuous.reading.kalmanVelocityVariance, recreated.reading.kalmanVelocityVariance)
        }
    }

    @Test
    fun timeoutStateSurvivesDuplicateAndRejectedReadingsAndClearsOnNewFix() {
        lateinit var pipeline: GPSPipeline
        lateinit var fireTimeout: () -> Unit
        var source = reading(1)
        val notifications = mutableListOf<Boolean>()
        val timeout = TimeoutGPSModule(
            notifyListeners = { notifications.add(pipeline.reading.isTimedOut) },
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
    fun grossJumpDoesNotChangeSmoothingOrCache() {
        val prefs = mock<UserPreferences> {
            on { filterLocationReadings }.thenReturn(true)
        }
        val pipeline = pipeline(BadReadingRejectionGPSModule(prefs, mock()), KalmanGPSModule(mock()))
        pipeline.update(reading(1))
        assertEquals(GPSUpdateResult.Rejected, pipeline.update(reading(2, 2.0)))
        assertEquals(reading(1).location, pipeline.reading.location)
        assertEquals(reading(1).location, pipeline().reading.location)
        assertEquals(GPSUpdateResult.NewFixAccepted, pipeline.update(reading(3, 1.0001)))
        assertTrue(pipeline.reading.location.longitude < 1.0001)
    }

    @Test
    fun initializesFromCacheAndReportsOnlyNewerReadingsAsChanges() {
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
    fun emptyCacheDoesNotChangeEmptyReading() {
        val pipeline = pipeline()
        assertEquals(Instant.EPOCH, pipeline.reading.time)
        assertEquals(Coordinate.zero, pipeline.reading.location)
        assertFalse(pipeline.reinitialize())
    }

    @Test
    fun initializationRunsEveryModuleEvenAfterAChange() {
        val calls = mutableListOf<Int>()
        fun initializer(id: Int) = object : GPSModule {
            override fun update(previousData: ModularGPSData, newData: ModularGPSData) = true
            override fun initialize(data: ModularGPSData): Boolean {
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
    fun forwardsLifecycleWithAcceptedReading() {
        val events = mutableListOf<String>()
        val pipeline = pipeline(object : GPSModule {
            override fun update(previousData: ModularGPSData, newData: ModularGPSData) = true

            override fun start(data: ModularGPSData) {
                events.add("start:${data.time.epochSecond}")
            }

            override fun stop(data: ModularGPSData) {
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
