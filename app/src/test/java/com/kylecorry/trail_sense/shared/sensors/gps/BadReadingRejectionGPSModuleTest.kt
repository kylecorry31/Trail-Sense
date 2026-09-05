package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.UserPreferences
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

class BadReadingRejectionGPSModuleTest {
    private val prefs = mock<UserPreferences> {
        on { filterLocationReadings }.thenReturn(true)
        on { requiresSatellites }.thenReturn(true)
    }
    private val module = BadReadingRejectionGPSModule(prefs, mock())
    private val time = Instant.parse("2020-01-01T00:00:00Z")

    private fun reading(seconds: Long = 0, longitude: Double = 1.0) = ModularGPSData(
        location = Coordinate(1.0, longitude), time = time.plusSeconds(seconds),
        hasValidReading = true, satellites = 4, horizontalAccuracy = 5f
    )

    @Test
    fun rejectsInvalidReadingsEvenWithFilteringDisabled() {
        whenever(prefs.filterLocationReadings).thenReturn(false)
        assertFalse(module.update(reading(), reading(1).apply { hasValidReading = false }))
        assertTrue(module.update(reading(), reading(-1, 50.0)))
    }

    @Test
    fun enforcesAccuracyLimitBeforeAcceptingFirstOrStaleReadings() {
        assertFalse(module.update(ModularGPSData(), reading().apply { horizontalAccuracy = 151f }))
        assertFalse(module.update(reading(), reading(121).apply { horizontalAccuracy = 151f }))
        assertTrue(module.update(reading(), reading(1).apply { horizontalAccuracy = 150f }))
    }

    @Test
    fun respectsSatelliteRequirementAndUnknownSatelliteCount() {
        assertFalse(module.update(reading(), reading(1).apply { satellites = 3 }))
        assertTrue(module.update(reading(), reading(1).apply { satellites = null }))
        whenever(prefs.requiresSatellites).thenReturn(false)
        assertTrue(module.update(reading(), reading(1).apply { satellites = 0 }))
    }

    @Test
    fun acceptsFirstReadingAndRecoversFromFuturePreviousTime() {
        assertTrue(module.update(ModularGPSData(), reading()))
        assertTrue(module.update(reading().apply { time = Instant.now().plusSeconds(60) }, reading()))
    }

    @Test
    fun rejectsOlderReadingsButAllowsSameTimestamp() {
        assertFalse(module.update(reading(), reading(-1)))
        assertTrue(module.update(reading(), reading()))
    }

    @Test
    fun rejectsImplausibleMovementUntilReadingIsStale() {
        assertFalse(module.update(reading(), reading(1, 2.0)))
        assertFalse(module.update(reading(), reading(120, 2.0)))
        assertTrue(module.update(reading(), reading(121, 2.0)))
    }

    @Test
    fun usesCandidateSpeedToAllowMovement() {
        val candidate = reading(1, 1.0002)
        assertFalse(module.update(reading(), candidate))
        candidate.speed = Speed.from(30f, DistanceUnits.Meters, TimeUnits.Seconds)
        assertTrue(module.update(reading(), candidate))
    }

    @Test
    fun doesNotModifyEitherReading() {
        val previous = reading()
        val candidate = reading(1, 2.0)
        assertFalse(module.update(previous, candidate))
        assertEquals(Coordinate(1.0, 1.0), previous.location)
        assertEquals(time, previous.time)
        assertEquals(Coordinate(1.0, 2.0), candidate.location)
        assertEquals(time.plusSeconds(1), candidate.time)
    }
}
