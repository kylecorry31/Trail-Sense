package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.sensors.gps.GPSPipeline
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import com.kylecorry.trail_sense.shared.sensors.gps.SpeedSource
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SpeedGPSModuleTest {
    private val module = SpeedGPSModule()
    private val previous = ModularGPSData()

    private fun reading(millis: Long, longitude: Double = 1.0, speed: Float = 0f) = ModularGPSData(
        location = Coordinate(1.0, longitude), eventTime = Instant.EPOCH.plusMillis(millis),
        eventTimeElapsedNanos = millis * 1_000_000,
        horizontalAccuracy = 1f,
        speed = Speed.from(speed, DistanceUnits.Meters, TimeUnits.Seconds)
    )

    @Test
    fun duplicateFixPreservesSpeedAfterHistoryEviction() = runBlocking<Unit> {
        val pipeline = GPSPipeline(listOf(SameFixGPSModule(), module))
        pipeline.update(reading(1000))
        for (second in 2L..10L) {
            pipeline.update(reading(second * 1000, 1.001, 1f))
        }
        pipeline.update(reading(11000))
        assertEquals(0f, pipeline.reading.speed.value)
        val source = pipeline.reading.speedSource
        val accuracy = pipeline.reading.speedAccuracy

        // The first update evicted the matching location at the start of the buffer.
        pipeline.update(reading(11000))

        assertEquals(0f, pipeline.reading.speed.value)
        assertEquals(source, pipeline.reading.speedSource)
        assertEquals(accuracy, pipeline.reading.speedAccuracy)
    }

    @Test
    fun restartDoesNotEstimateSpeedFromPreviousSession() = runBlocking<Unit> {
        val pipeline = GPSPipeline(listOf(SameFixGPSModule(), module))
        pipeline.start()
        pipeline.update(reading(1000))
        pipeline.stop()
        pipeline.start()
        // A repeated startup fix must not seed the new session with the old location.
        pipeline.update(reading(1000))
        pipeline.update(reading(3601000, 1.1))
        assertEquals(0f, pipeline.reading.speed.value)

        // Fresh history still supports estimates within the new session.
        pipeline.update(reading(3611000, 1.101))
        assertTrue(pipeline.reading.speed.value in 10f..12f)
        pipeline.stop()
    }

    @Test
    fun keepsFirstReadingSpeedWithoutHistory() = runBlocking<Unit> {
        val candidate = reading(0)
        assertTrue(module.update(previous, candidate))
        assertEquals(0f, candidate.speed.value)
    }

    @Test
    fun preservesReportedNonzeroSpeed() = runBlocking<Unit> {
        module.update(previous, reading(0))
        val candidate = reading(1000, 1.001, 3f).apply {
            speedSource = SpeedSource.Provider
            speedAccuracy = 0.2f
        }
        module.update(previous, candidate)
        assertEquals(3f, candidate.speed.value)
        assertEquals(SpeedSource.Provider, candidate.speedSource)
        assertEquals(0.2f, candidate.speedAccuracy)
    }

    @Test
    fun estimatesMissingSpeedFromMovementAndElapsedTime() = runBlocking<Unit> {
        module.update(previous, reading(0))
        val candidate = reading(10000, 1.001).apply {
            speedSource = SpeedSource.Provider
            speedAccuracy = 0.2f
        }
        module.update(previous, candidate)
        assertTrue(candidate.speed.value in 10f..12f)
        assertEquals(SpeedSource.PositionDerived, candidate.speedSource)
        assertNull(candidate.speedAccuracy)
        assertEquals(0f, previous.speed.value)
        assertEquals(Coordinate.zero, previous.location)
    }

    @Test
    fun estimatesZeroSpeedWithoutSpeedAccuracyFromMovement() = runBlocking<Unit> {
        module.update(previous, reading(0))
        val candidate = reading(10000, 1.001).apply {
            speedSource = SpeedSource.Provider
            speedAccuracy = null
        }

        module.update(previous, candidate)

        assertTrue(candidate.speed.value in 10f..12f)
        assertEquals(SpeedSource.PositionDerived, candidate.speedSource)
        assertNull(candidate.speedAccuracy)
    }

    @Test
    fun estimatesSpeedFromElapsedTime() = runBlocking<Unit> {
        module.update(previous, reading(0))
        // The fix time says 10 seconds passed, but only 5 seconds have elapsed
        val candidate = reading(5000, 1.001).apply {
            eventTime = Instant.EPOCH.plusSeconds(10)
            speedAccuracy = 0.2f
        }
        module.update(previous, candidate)
        assertTrue(candidate.speed.value in 20f..24f)
    }

    @Test
    fun keepsSpeedZeroWhenMovementIsWithinAccuracy() = runBlocking<Unit> {
        module.update(previous, reading(0))
        val candidate = reading(1000, 1.000001)
        module.update(previous, candidate)
        assertEquals(0f, candidate.speed.value)
    }

    @Test
    fun frequentUpdatesDoNotEvictHistoryBeforeOneSecond() = runBlocking<Unit> {
        module.update(previous, reading(0))
        for (millis in 1L..20L) {
            module.update(previous, reading(millis, 1.001, 1f))
        }
        val candidate = reading(10000, 1.001).apply {
            speedAccuracy = 0.2f
        }
        module.update(previous, candidate)
        assertTrue(candidate.speed.value in 10f..12f)
    }
}
