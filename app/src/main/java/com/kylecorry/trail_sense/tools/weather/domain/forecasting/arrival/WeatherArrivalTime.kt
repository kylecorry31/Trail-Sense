package com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival

import com.kylecorry.trail_sense.tools.weather.domain.RelativeArrivalTime
import java.time.Duration
import java.time.Instant

data class WeatherArrivalTime(val time: Instant, val isExact: Boolean) {
    fun toRelative(now: Instant): RelativeArrivalTime {
        return if (Duration.between(now, time) <= SOON_WINDOW) RelativeArrivalTime.Soon else RelativeArrivalTime.Later
    }

    companion object {
        val SOON_WINDOW: Duration = Duration.ofHours(3)
        val LATER_WINDOW: Duration = Duration.ofHours(12)

        fun fromRelative(now: Instant, approximate: RelativeArrivalTime): WeatherArrivalTime {
            val duration = when (approximate) {
                RelativeArrivalTime.Soon -> Duration.ofHours(2)
                // WHY: Weather forecast as later usually arrived 3 to 5 hours out at the benchmark stations
                RelativeArrivalTime.Later -> Duration.ofHours(4)
            }
            return WeatherArrivalTime(now.plus(duration), false)
        }
    }
}
