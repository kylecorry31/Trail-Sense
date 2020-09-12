package com.kylecorry.trail_sense.astronomy.domain

import java.time.Duration
import java.time.LocalDateTime

object DateUtils {
    fun getClosestPastTime(
        currentTime: LocalDateTime,
        times: List<LocalDateTime?>
    ): LocalDateTime? {
        return times.filterNotNull().filter { it.isBefore(currentTime) }
            .minByOrNull { Duration.between(it, currentTime).abs() }
    }

    fun getClosestFutureTime(
        currentTime: LocalDateTime,
        times: List<LocalDateTime?>
    ): LocalDateTime? {
        return times.filterNotNull().filter { it.isAfter(currentTime) }
            .minByOrNull { Duration.between(it, currentTime).abs() }
    }
}