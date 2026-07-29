package com.kylecorry.trail_sense.tools.weather.domain

import com.kylecorry.luna.specifications.Specification
import java.time.LocalTime

class CanSendDailyForecast(private val startTime: LocalTime) : Specification<LocalTime>() {
    override fun isSatisfiedBy(value: LocalTime): Boolean {
        val end = startTime.plusHours(3)
        if (end >= startTime) {
            return value in startTime..end
        }

        // start is after end, therefore it starts night and ends in the morning
        return value in startTime..LocalTime.MAX || value in LocalTime.MIN..end
    }
}
