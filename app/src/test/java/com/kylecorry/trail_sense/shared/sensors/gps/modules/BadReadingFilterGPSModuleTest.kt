package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class BadReadingFilterGPSModuleTest {
    private val module = BadReadingFilterGPSModule(mock())
    private val time = Instant.parse("2020-01-01T00:00:00Z")

    private fun reading(seconds: Long = 0, longitude: Double = 1.0) = ModularGPSData(
        location = Coordinate(1.0, longitude), time = time.plusSeconds(seconds),
        hasValidReading = true
    )

    @Test
    fun rejectsReadingsWithoutAValidFix() = runBlocking<Unit> {
        assertFalse(module.update(reading(), reading(1).apply { hasValidReading = false }))
    }

    @Test
    fun acceptsFirstReadingAndRecoversFromFuturePreviousTime() = runBlocking<Unit> {
        assertTrue(module.update(ModularGPSData(), reading()))
        assertTrue(module.update(reading().apply { time = Instant.now().plusSeconds(60) }, reading()))
    }

    @Test
    fun rejectsOlderReadingsButAllowsSameTimestamp() = runBlocking<Unit> {
        assertFalse(module.update(reading(), reading(-1)))
        assertTrue(module.update(reading(), reading()))
    }

    @Test
    fun doesNotModifyEitherReading() = runBlocking<Unit> {
        val previous = reading()
        val candidate = reading(-1, 2.0)
        assertFalse(module.update(previous, candidate))
        assertEquals(Coordinate(1.0, 1.0), previous.location)
        assertEquals(time, previous.time)
        assertEquals(Coordinate(1.0, 2.0), candidate.location)
        assertEquals(time.minusSeconds(1), candidate.time)
    }
}
