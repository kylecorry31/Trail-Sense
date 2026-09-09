package com.kylecorry.trail_sense.tools.battery.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class PowerServiceTest {

    private val service = PowerService()
    private val start = Instant.ofEpochMilli(1000000)

    @Test
    fun deepSleepIsTheElapsedTimeNotSpentAwake() {
        val readings = listOf(
            reading(Duration.ZERO, uptime = Duration.ofMinutes(30), elapsed = Duration.ofMinutes(30)),
            reading(Duration.ofHours(1), uptime = Duration.ofMinutes(45), elapsed = Duration.ofMinutes(90))
        )

        val deepSleep = service.getDeepSleep(readings)!!
        assertEquals(75f, deepSleep.percent, 0.01f)
        assertEquals(Duration.ofMinutes(60), deepSleep.duration)
    }

    @Test
    fun deepSleepIsAveragedOverAllIntervals() {
        val readings = listOf(
            reading(Duration.ZERO, uptime = Duration.ZERO, elapsed = Duration.ZERO),
            // Fully awake for an hour
            reading(Duration.ofHours(1), uptime = Duration.ofHours(1), elapsed = Duration.ofHours(1)),
            // Fully asleep for an hour
            reading(Duration.ofHours(2), uptime = Duration.ofHours(1), elapsed = Duration.ofHours(2))
        )

        val deepSleep = service.getDeepSleep(readings)!!
        assertEquals(50f, deepSleep.percent, 0.01f)
        assertEquals(Duration.ofHours(2), deepSleep.duration)
    }

    @Test
    fun deepSleepIgnoresIntervalsWhereTheClocksReset() {
        val readings = listOf(
            reading(Duration.ZERO, uptime = Duration.ofHours(10), elapsed = Duration.ofHours(20)),
            // Reboot - the clocks restart, so this interval can't be used
            reading(Duration.ofHours(1), uptime = Duration.ofMinutes(15), elapsed = Duration.ofMinutes(15)),
            reading(Duration.ofHours(2), uptime = Duration.ofMinutes(30), elapsed = Duration.ofMinutes(75))
        )

        // Only the post-reboot interval counts, so the duration covers that hour alone
        val deepSleep = service.getDeepSleep(readings)!!
        assertEquals(75f, deepSleep.percent, 0.01f)
        assertEquals(Duration.ofMinutes(60), deepSleep.duration)
    }

    @Test
    fun deepSleepIsNullWithoutAComparableInterval() {
        assertNull(service.getDeepSleep(emptyList()))
        assertNull(
            service.getDeepSleep(
                listOf(reading(Duration.ZERO, Duration.ZERO, Duration.ZERO))
            )
        )
        // Readings recorded before the clocks were tracked
        assertNull(
            service.getDeepSleep(
                listOf(
                    BatteryReading(start, 100f, 0f, false),
                    BatteryReading(start.plus(Duration.ofHours(1)), 90f, 0f, false)
                )
            )
        )
    }

    private fun reading(
        offset: Duration,
        uptime: Duration,
        elapsed: Duration
    ): BatteryReading {
        return BatteryReading(start.plus(offset), 100f, 0f, false, uptime, elapsed)
    }
}
