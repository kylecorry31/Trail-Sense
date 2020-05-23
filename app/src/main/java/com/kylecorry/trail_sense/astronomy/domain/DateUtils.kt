package com.kylecorry.trail_sense.astronomy.domain

import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime

object DateUtils {
    fun getClosestPastTime(
        currentTime: LocalDateTime,
        times: List<LocalDateTime?>
    ): LocalDateTime? {
        return times.filterNotNull().filter { it.isBefore(currentTime) }
            .minBy { Duration.between(it, currentTime).abs() }
    }

    fun getClosestFutureTime(
        currentTime: LocalDateTime,
        times: List<LocalDateTime?>
    ): LocalDateTime? {
        return times.filterNotNull().filter { it.isAfter(currentTime) }
            .minBy { Duration.between(it, currentTime).abs() }
    }
}