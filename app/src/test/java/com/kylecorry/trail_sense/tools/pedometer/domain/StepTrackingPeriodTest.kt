package com.kylecorry.trail_sense.tools.pedometer.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
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

    @Test
    fun elapsedTimeIsTimeBetweenStartAndEnd() {
        val period = period(
            startTime = Instant.parse("2026-01-01T10:00:00Z"),
            endTime = Instant.parse("2026-01-01T11:30:00Z")
        )

        assertEquals(Duration.ofMinutes(90), period.elapsedTime)
    }

    @Test
    fun activeTimeIsTheSumOfBucketActiveTimes() {
        val period = period(
            endTime = Instant.parse("2026-01-01T01:00:00Z"),
            stepCountBuckets = listOf(
                bucket(id = 1, steps = 100, activeTime = Duration.ofMinutes(10)),
                bucket(id = 2, steps = 200, activeTime = Duration.ofMinutes(15))
            )
        )

        assertEquals(Duration.ofMinutes(25), period.activeTime)
    }

    @Test
    fun activeTimeUsesBucketDurationWhenBucketHasStepsAndNoActiveTime() {
        val period = period(
            endTime = Instant.parse("2026-01-01T01:00:00Z"),
            stepCountBuckets = listOf(
                bucket(
                    steps = 100,
                    startTime = Instant.parse("2026-01-01T00:10:00Z"),
                    endTime = Instant.parse("2026-01-01T00:40:00Z")
                )
            )
        )

        assertEquals(Duration.ofMinutes(30), period.activeTime)
    }

    @Test
    fun activeTimeDoesNotUseBucketDurationWhenBucketHasNoSteps() {
        val period = period(
            endTime = Instant.parse("2026-01-01T01:00:00Z"),
            stepCountBuckets = listOf(bucket(steps = 0))
        )

        assertEquals(Duration.ZERO, period.activeTime)
    }

    @Test
    fun activeTimeCapsFallbackBucketEndTimeToCurrentTime() {
        val currentTime = Instant.now()
        val period = period(
            startTime = currentTime.minus(Duration.ofHours(1)),
            endTime = currentTime.plus(Duration.ofHours(1)),
            stepCountBuckets = listOf(
                bucket(
                    steps = 100,
                    startTime = currentTime.minus(Duration.ofMinutes(30)),
                    endTime = currentTime.plus(Duration.ofMinutes(30))
                )
            )
        )

        assertTrue(period.activeTime >= Duration.ofMinutes(30))
        assertTrue(period.activeTime < Duration.ofMinutes(31))
    }

    @Test
    fun activeTimeIsCappedToElapsedTime() {
        val period = period(
            startTime = Instant.parse("2026-01-01T00:15:00Z"),
            endTime = Instant.parse("2026-01-01T00:45:00Z"),
            stepCountBuckets = listOf(
                bucket(
                    steps = 100,
                    startTime = Instant.parse("2026-01-01T00:00:00Z"),
                    endTime = Instant.parse("2026-01-01T01:00:00Z")
                )
            )
        )

        assertEquals(Duration.ofMinutes(30), period.activeTime)
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
        steps: Long = 0,
        activeTime: Duration = Duration.ZERO
    ): StepCountBucket {
        return StepCountBucket(id, periodId, startTime, endTime, steps, activeTime)
    }
}
