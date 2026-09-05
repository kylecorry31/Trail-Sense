package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class SpeedGPSModuleTest {
    private val module = SpeedGPSModule()
    private val previous = ModularGPSData()

    private fun reading(millis: Long, longitude: Double = 1.0, speed: Float = 0f) = ModularGPSData(
        location = Coordinate(1.0, longitude), time = Instant.EPOCH.plusMillis(millis),
        horizontalAccuracy = 1f,
        speed = Speed.from(speed, DistanceUnits.Meters, TimeUnits.Seconds)
    )

    @Test
    fun keepsFirstReadingSpeedWithoutHistory() {
        val candidate = reading(0)
        assertTrue(module.update(previous, candidate))
        assertEquals(0f, candidate.speed.value)
    }

    @Test
    fun preservesReportedNonzeroSpeed() {
        module.update(previous, reading(0))
        val candidate = reading(1000, 1.001, 3f)
        module.update(previous, candidate)
        assertEquals(3f, candidate.speed.value)
    }

    @Test
    fun estimatesMissingSpeedFromMovementAndElapsedTime() {
        module.update(previous, reading(0))
        val candidate = reading(10000, 1.001)
        module.update(previous, candidate)
        assertTrue(candidate.speed.value in 10f..12f)
        assertEquals(0f, previous.speed.value)
        assertEquals(Coordinate.zero, previous.location)
    }

    @Test
    fun keepsSpeedZeroWhenMovementIsWithinAccuracy() {
        module.update(previous, reading(0))
        val candidate = reading(1000, 1.000001)
        module.update(previous, candidate)
        assertEquals(0f, candidate.speed.value)
    }

    @Test
    fun frequentUpdatesDoNotEvictHistoryBeforeOneSecond() {
        module.update(previous, reading(0))
        for (millis in 1L..20L) {
            module.update(previous, reading(millis, 1.001, 1f))
        }
        val candidate = reading(10000, 1.001)
        module.update(previous, candidate)
        assertTrue(candidate.speed.value in 10f..12f)
    }
}
