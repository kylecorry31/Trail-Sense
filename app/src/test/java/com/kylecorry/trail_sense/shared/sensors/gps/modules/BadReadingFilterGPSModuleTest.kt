package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.andromeda.core.time.TimeProvider
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class BadReadingFilterGPSModuleTest {
    private var elapsedMillis = 200_000L
    private val timeProvider = object : TimeProvider {
        override fun elapsedRealtime() = elapsedMillis
        override fun currentTimeMillis() = System.currentTimeMillis()
    }
    private val module = BadReadingFilterGPSModule(mock(), timeProvider)
    private val time = Instant.parse("2020-01-01T00:00:00Z")

    private fun reading(seconds: Long = 0, longitude: Double = 1.0) = ModularGPSData(
        location = Coordinate(1.0, longitude), eventTime = time.plusSeconds(seconds),
        eventTimeElapsedNanos = (100 + seconds) * 1_000_000_000,
        hasValidReading = true
    )

    @Test
    fun rejectsReadingsWithoutAValidFix() = runBlocking<Unit> {
        assertFalse(module.update(reading(), reading(1).apply { hasValidReading = false }))
    }

    @Test
    fun acceptsFirstReading() = runBlocking<Unit> {
        assertTrue(module.update(ModularGPSData(), reading()))
    }

    @Test
    fun acceptsAValidReadingWhenThePreviousFixIsInTheFuture() = runBlocking<Unit> {
        val previous = reading().apply { eventTimeElapsedNanos = 201_000_000_000L }
        val current = reading().apply { eventTimeElapsedNanos = 200_000_000_000L }

        assertTrue(module.update(previous, current))
    }

    @Test
    fun rejectsOlderReadingsButAllowsSameTimestamp() = runBlocking<Unit> {
        assertFalse(module.update(reading(), reading(-1)))
        assertTrue(module.update(reading(), reading()))
    }

    @Test
    fun ordersReadingsByElapsedTime() = runBlocking<Unit> {
        val previous = reading()
        val clockMovedBack = reading(1).apply { eventTime = time.minusSeconds(60) }
        assertTrue(module.update(previous, clockMovedBack))
        val clockMovedForward = reading(-1).apply { eventTime = time.plusSeconds(60) }
        assertFalse(module.update(previous, clockMovedForward))
    }

    @Test
    fun doesNotModifyEitherReading() = runBlocking<Unit> {
        val previous = reading()
        val candidate = reading(-1, 2.0)
        assertFalse(module.update(previous, candidate))
        assertEquals(Coordinate(1.0, 1.0), previous.location)
        assertEquals(time, previous.eventTime)
        assertEquals(Coordinate(1.0, 2.0), candidate.location)
        assertEquals(time.minusSeconds(1), candidate.eventTime)
    }
}
