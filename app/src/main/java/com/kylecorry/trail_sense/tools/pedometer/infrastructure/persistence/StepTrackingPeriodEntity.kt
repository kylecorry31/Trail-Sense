package com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kylecorry.trail_sense.tools.pedometer.domain.StepCountBucket
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod
import java.time.Instant

@Entity(
    tableName = "step_tracking_periods",
    indices = [Index(value = ["end_time"])]
)
data class StepTrackingPeriodEntity(
    @ColumnInfo(name = "start_time") val startTime: Instant,
    @ColumnInfo(name = "end_time") val endTime: Instant?
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toStepTrackingPeriod(buckets: List<StepCountBucket>): StepTrackingPeriod {
        return StepTrackingPeriod(
            id = id,
            startTime = startTime,
            endTime = endTime,
            stepCountBuckets = buckets
        )
    }

    companion object {
        fun from(period: StepTrackingPeriod): StepTrackingPeriodEntity {
            return StepTrackingPeriodEntity(
                startTime = period.startTime,
                endTime = period.endTime
            ).also {
                it.id = period.id
            }
        }
    }
}
