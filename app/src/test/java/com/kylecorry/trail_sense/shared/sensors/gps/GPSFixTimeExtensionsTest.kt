package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.core.time.TimeProvider
import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GPSFixTimeExtensionsTest {

    private fun reading(seconds: Long, elapsedSeconds: Long) = ModularGPSData(
        eventTime = Instant.EPOCH.plusSeconds(seconds),
        eventTimeElapsedNanos = elapsedSeconds * 1_000_000_000
    )

    @Test
    fun durationSinceUsesElapsedRealtime() {
        assertEquals(Duration.ofSeconds(2), reading(50, 3).durationSince(reading(100, 1)))
        assertEquals(Duration.ofSeconds(-2), reading(100, 1).durationSince(reading(50, 3)))
        assertEquals(Duration.ofSeconds(4), reading(0, 3).durationSince(-1_000_000_000))
    }

    @Test
    fun ageUsesElapsedRealtime() {
        val timeProvider = object : TimeProvider {
            override fun elapsedRealtime() = 10_000L
            override fun currentTimeMillis() = 500_000L
        }
        assertEquals(Duration.ofSeconds(7), reading(1000, 3).age(timeProvider))
    }
}
