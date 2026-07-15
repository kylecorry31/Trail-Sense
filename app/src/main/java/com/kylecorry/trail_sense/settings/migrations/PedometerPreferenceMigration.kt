package com.kylecorry.trail_sense.settings.migrations

import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.tools.pedometer.domain.ActiveTimeCalculator
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class PedometerPreferenceMigration(
    private val stepTracker: IStepTrackerService,
    private val prefs: IPreferences,
    private val now: () -> Instant = Instant::now
) {
    private val activeTimeCalculator = ActiveTimeCalculator()

    suspend fun migrate(): Unit = onIO {
        val steps = prefs.getLong(STEPS_KEY) ?: 0L
        val startTime = prefs.getInstant(LAST_RESET_KEY)
        if (steps > 0 && startTime != null) {
            var remainingSteps = steps
            createStepAdditions(startTime, now().coerceAtLeast(startTime), steps).forEach {
                stepTracker.addSteps(
                    it.steps,
                    it.time,
                    activeTimeCalculator.calculate(it.steps, it.elapsedTime)
                )
                remainingSteps -= it.steps
                prefs.putLong(STEPS_KEY, remainingSteps)
                prefs.putInstant(LAST_RESET_KEY, it.endTime)
            }
        }
        prefs.remove(STEPS_KEY)
        prefs.remove(LAST_RESET_KEY)
    }

    private fun createStepAdditions(
        startTime: Instant,
        endTime: Instant,
        steps: Long
    ): List<StepAddition> {
        val bucketRanges = getStepCountBucketRanges(startTime, endTime)
        val baseSteps = steps / bucketRanges.size
        val remainingSteps = steps % bucketRanges.size
        return bucketRanges.mapIndexed { index, range ->
            StepAddition(
                time = range.first,
                steps = baseSteps + if (index.toLong() < remainingSteps) 1 else 0,
                elapsedTime = Duration.between(range.first, minOf(range.second, endTime)),
                endTime = minOf(range.second, endTime)
            )
        }
    }

    private fun getStepCountBucketRanges(
        startTime: Instant,
        endTime: Instant
    ): List<Pair<Instant, Instant>> {
        val ranges = mutableListOf<Pair<Instant, Instant>>()
        var bucketStart = startTime
        do {
            val nextHour = bucketStart.toZonedDateTime()
                .truncatedTo(ChronoUnit.HOURS)
                .plus(Duration.ofHours(1))
                .toInstant()
            ranges.add(bucketStart to nextHour)
            bucketStart = nextHour
        } while (bucketStart.isBefore(endTime))
        return ranges
    }

    private data class StepAddition(
        val time: Instant,
        val steps: Long,
        val elapsedTime: Duration,
        val endTime: Instant
    )

    companion object {
        private const val STEPS_KEY = "cache_steps"
        private const val LAST_RESET_KEY = "last_odometer_reset"
    }
}
