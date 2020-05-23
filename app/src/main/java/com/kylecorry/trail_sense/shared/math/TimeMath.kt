package com.kylecorry.trail_sense.shared.math

import org.threeten.bp.Duration
import org.threeten.bp.temporal.Temporal

fun getPercentOfDuration(start: Temporal, end: Temporal, current: Temporal): Float {
    val duration = Duration.between(start, end).seconds
    val elapsed = Duration.between(start, current).seconds
    return elapsed / duration.toFloat()
}