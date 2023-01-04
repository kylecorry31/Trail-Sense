package com.kylecorry.trail_sense.weather.domain.forecasting.arrival

import com.kylecorry.trail_sense.weather.domain.RelativeArrivalTime
import java.time.Duration
import java.time.Instant

data class WeatherArrivalTime(val time: Instant, val isExact: Boolean) {
    fun toRelative(now: Instant): RelativeArrivalTime {
        val timeUntil = Duration.between(now, time)
        return when {
            timeUntil <= Duration.ofMinutes(10) -> RelativeArrivalTime.Now
            timeUntil <= Duration.ofHours(1) -> RelativeArrivalTime.VerySoon
            timeUntil <= Duration.ofHours(2) -> RelativeArrivalTime.Soon
            else -> RelativeArrivalTime.Later
        }
    }

    companion object {
        fun fromRelative(now: Instant, approximate: RelativeArrivalTime): WeatherArrivalTime {
            val duration = when (approximate) {
                RelativeArrivalTime.Now -> Duration.ZERO
                RelativeArrivalTime.VerySoon -> Duration.ofHours(1)
                RelativeArrivalTime.Soon -> Duration.ofHours(2)
                RelativeArrivalTime.Later -> Duration.ofHours(8)
            }
            return WeatherArrivalTime(now.plus(duration), false)
        }
    }
}