package com.kylecorry.trail_sense.settings.migrations

import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class PedometerPreferenceMigration(
    private val stepTracker: IStepTrackerService,
    private val prefs: IPreferences,
    private val now: () -> Instant = Instant::now
) {

    suspend fun migrate() {
        val steps = prefs.getLong(STEPS_KEY) ?: 0L
        val startTime = prefs.getInstant(LAST_RESET_KEY)
        if (steps > 0 && startTime != null) {
            createStepAdditions(startTime, now(), steps).forEach {
                stepTracker.addSteps(it.steps, it.time)
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
                steps = baseSteps + if (index.toLong() < remainingSteps) 1 else 0
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
        val steps: Long
    )

    companion object {
        private const val STEPS_KEY = "cache_steps"
        private const val LAST_RESET_KEY = "last_odometer_reset"
    }
}
