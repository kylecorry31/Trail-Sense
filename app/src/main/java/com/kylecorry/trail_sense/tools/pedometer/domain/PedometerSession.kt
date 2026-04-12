package com.kylecorry.trail_sense.tools.pedometer.domain

import java.time.Duration
import java.time.Instant

// #1397: Domain model for a completed pedometer session
data class PedometerSession(
    val id: Long,
    val startTime: Instant,
    val endTime: Instant,
    val steps: Long,
    val distance: Float
) {
    val duration: Duration get() = Duration.between(startTime, endTime)
}
