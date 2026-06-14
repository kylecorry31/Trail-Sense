package com.kylecorry.trail_sense.tools.pedometer.domain

import java.time.Instant

data class HourlyStepCount(
    val startTime: Instant,
    val endTime: Instant,
    val steps: Long
)
