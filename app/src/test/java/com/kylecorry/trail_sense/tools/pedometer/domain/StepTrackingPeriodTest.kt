package com.kylecorry.trail_sense.tools.pedometer.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

internal class StepTrackingPeriodTest {

    @Test
    fun isOpenIsTrueWhenEndTimeIsNull() {
        val period = period(endTime = null)
        assertTrue(period.isOpen)
    }

    @Test
    fun isOpenIsFalseWhenEndTimeIsSet() {
        val period = period(endTime = Instant.parse("2026-01-01T12:00:00Z"))
        assertFalse(period.isOpen)
    }

    @Test
    fun stepsIsZeroWhenThereAreNoBuckets() {
        val period = period(stepCountBuckets = emptyList())
        assertEquals(0L, period.steps)
    }

    @Test
    fun stepsIsTheSumOfAllBucketSteps() {
        val period = period(
            stepCountBuckets = listOf(
                bucket(id = 1, steps = 100),
                bucket(id = 2, steps = 250),
                bucket(id = 3, steps = 0)
            )
        )
        assertEquals(350L, period.steps)
    }

    private fun period(
        id: Long = 1,
        startTime: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        endTime: Instant? = null,
        stepCountBuckets: List<StepCountBucket> = emptyList()
    ): StepTrackingPeriod {
        return StepTrackingPeriod(id, startTime, endTime, stepCountBuckets)
    }

    private fun bucket(
        id: Long = 1,
        periodId: Long = 1,
        startTime: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        endTime: Instant = Instant.parse("2026-01-01T00:01:00Z"),
        steps: Long = 0
    ): StepCountBucket {
        return StepCountBucket(id, periodId, startTime, endTime, steps)
    }
}
