package com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import java.time.Instant

@Dao
interface StepTrackerDao {

    @Query("SELECT * FROM step_tracking_periods ORDER BY start_time DESC")
    suspend fun getStepTrackingPeriods(): List<StepTrackingPeriodEntity>

    @Query("SELECT * FROM step_tracking_periods WHERE end_time IS NULL ORDER BY start_time DESC LIMIT 1")
    suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriodEntity?

    @Upsert
    suspend fun upsert(period: StepTrackingPeriodEntity): Long

    @Delete
    suspend fun delete(period: StepTrackingPeriodEntity)

    @Query("SELECT * FROM step_count_buckets WHERE period_id = :periodId ORDER BY start_time")
    suspend fun getStepCountBuckets(periodId: Long): List<StepCountBucketEntity>

    @Query(
        "SELECT * FROM step_count_buckets " +
            "WHERE start_time < :endTime AND end_time > :startTime " +
            "ORDER BY start_time"
    )
    suspend fun getStepCountBuckets(startTime: Instant, endTime: Instant): List<StepCountBucketEntity>

    @Upsert
    suspend fun upsert(bucket: StepCountBucketEntity): Long

    @Query("DELETE FROM step_count_buckets WHERE period_id = :periodId")
    suspend fun deleteBucketsInPeriod(periodId: Long)

    @Query("DELETE FROM step_count_buckets WHERE end_time < :endTime")
    suspend fun deleteBucketsOlderThan(endTime: Instant)

    @Query(
        "DELETE FROM step_tracking_periods " +
            "WHERE end_time IS NOT NULL " +
            "AND _id NOT IN (SELECT DISTINCT period_id FROM step_count_buckets)"
    )
    suspend fun deleteEmptyClosedPeriods()
}
