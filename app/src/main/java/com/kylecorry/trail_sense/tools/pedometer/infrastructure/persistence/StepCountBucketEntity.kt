package com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kylecorry.trail_sense.tools.pedometer.domain.StepCountBucket
import java.time.Duration
import java.time.Instant

@Entity(
    tableName = "step_count_buckets",
    indices = [Index(value = ["period_id"])]
)
data class StepCountBucketEntity(
    @ColumnInfo(name = "period_id") val periodId: Long,
    @ColumnInfo(name = "start_time") val startTime: Instant,
    @ColumnInfo(name = "end_time") val endTime: Instant,
    @ColumnInfo(name = "steps") val steps: Long,
    @ColumnInfo(name = "active_time") val activeTime: Long
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toStepCountBucket(): StepCountBucket {
        return StepCountBucket(
            id = id,
            periodId = periodId,
            startTime = startTime,
            endTime = endTime,
            steps = steps,
            activeTime = Duration.ofMillis(activeTime)
        )
    }

    companion object {
        fun from(bucket: StepCountBucket): StepCountBucketEntity {
            return StepCountBucketEntity(
                periodId = bucket.periodId,
                startTime = bucket.startTime,
                endTime = bucket.endTime,
                steps = bucket.steps,
                activeTime = bucket.activeTime.toMillis()
            ).also {
                it.id = bucket.id
            }
        }
    }
}
