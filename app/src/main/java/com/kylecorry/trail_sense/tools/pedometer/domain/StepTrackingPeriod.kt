package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.trail_sense.shared.data.Identifiable
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
}
