package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.trail_sense.shared.data.Identifiable
import java.time.Duration
import java.time.Instant

data class StepTrackingPeriod(
    override val id: Long,
    val startTime: Instant,
    val endTime: Instant?,
    val stepCountBuckets: List<StepCountBucket>
) : Identifiable {
    val isOpen = endTime == null
    val steps: Long
        get() = stepCountBuckets.sumOf { it.steps }

    val elapsedTime: Duration
        get() = Duration.between(startTime, endTime ?: Instant.now())

    val activeTime: Duration
        get() {
            val currentTime = Instant.now()
            val totalActiveTime = stepCountBuckets.fold(Duration.ZERO) { total, bucket ->
                val bucketActiveTime = if (bucket.steps > 0 && bucket.activeTime.isZero) {
                    Duration.between(bucket.startTime, minOf(bucket.endTime, currentTime))
                } else {
                    bucket.activeTime
                }
                total.plus(bucketActiveTime)
            }
            return minOf(totalActiveTime, elapsedTime)
        }
}
