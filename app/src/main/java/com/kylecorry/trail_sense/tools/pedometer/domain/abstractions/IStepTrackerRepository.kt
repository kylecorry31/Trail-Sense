package com.kylecorry.trail_sense.tools.pedometer.domain.abstractions

import com.kylecorry.trail_sense.tools.pedometer.domain.StepCountBucket
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod
import java.time.Instant

interface IStepTrackerRepository {
    // Step tracking periods
    suspend fun getStepTrackingPeriods(): List<StepTrackingPeriod>
    suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriod?
    suspend fun upsertStepTrackingPeriod(period: StepTrackingPeriod): Long
    suspend fun deleteStepTrackingPeriod(period: StepTrackingPeriod)
    suspend fun setMinimumPeriodStartTime(startTime: Instant): Boolean

    // Step count buckets
    suspend fun getStepCountBuckets(startTime: Instant, endTime: Instant): List<StepCountBucket>
    suspend fun upsertStepCountBucket(bucket: StepCountBucket): Long
    suspend fun deleteBucketsInPeriod(periodId: Long)
    suspend fun deleteBucketsOlderThan(endTime: Instant): Boolean
    suspend fun deleteEmptyClosedPeriods(): Boolean
}
