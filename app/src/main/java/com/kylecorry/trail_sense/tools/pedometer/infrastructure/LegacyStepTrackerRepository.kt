package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.pedometer.domain.StepCountBucket
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod
import com.kylecorry.trail_sense.tools.pedometer.domain.abstractions.IStepTrackerRepository
import java.time.Instant

internal class LegacyStepTrackerRepository : IStepTrackerRepository {

    private val counter = StepCounter(getAppService<PreferencesSubsystem>().preferences)

    override suspend fun getStepTrackingPeriods(): List<StepTrackingPeriod> {
        val start = counter.startTime ?: return emptyList()
        val steps = counter.steps
        val bucket = StepCountBucket(
            id = 1,
            periodId = 1,
            startTime = start,
            endTime = Instant.MAX,
            steps = steps
        )
        return listOf(
            StepTrackingPeriod(
                id = 1,
                startTime = start,
                endTime = null,
                stepCountBuckets = listOf(bucket)
            )
        )
    }

    override suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriod? {
        return getStepTrackingPeriods().firstOrNull()
    }

    override suspend fun upsertStepTrackingPeriod(period: StepTrackingPeriod): Long {
        counter.reset()
        return 1L
    }

    override suspend fun deleteStepTrackingPeriod(period: StepTrackingPeriod) {
        counter.reset()
    }

    override suspend fun upsertStepCountBucket(bucket: StepCountBucket): Long {
        counter.addSteps(bucket.steps - counter.steps)
        return 1L
    }

    override suspend fun deleteBucketsInPeriod(periodId: Long) {
        counter.reset()
    }
}
