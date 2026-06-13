package com.kylecorry.trail_sense.tools.pedometer.domain.abstractions

import com.kylecorry.trail_sense.tools.pedometer.domain.StepCountBucket
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod

interface IStepTrackerRepository {
    // Step tracking periods
    suspend fun getStepTrackingPeriods(): List<StepTrackingPeriod>
    suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriod?
    suspend fun upsertStepTrackingPeriod(period: StepTrackingPeriod): Long
    suspend fun deleteStepTrackingPeriod(period: StepTrackingPeriod)

    // Step count buckets
    suspend fun upsertStepCountBucket(bucket: StepCountBucket): Long
    suspend fun deleteBucketsInPeriod(periodId: Long)
}
