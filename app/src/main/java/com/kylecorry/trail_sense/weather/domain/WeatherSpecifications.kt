package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.andromeda.core.specifications.Specification
import java.time.LocalTime

class CanSendDailyForecast(private val startTime: LocalTime) : Specification<LocalTime>() {
    override fun isSatisfiedBy(value: LocalTime): Boolean {
        val end = startTime.plusHours(3)
        return value >= startTime && value <= end
    }
}
