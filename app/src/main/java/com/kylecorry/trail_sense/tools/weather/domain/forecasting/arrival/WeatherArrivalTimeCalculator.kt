package com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival

import com.kylecorry.sol.time.Time.roundNearestMinute
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.tools.weather.domain.RelativeArrivalTime
import java.time.Clock
import java.time.Instant

internal class WeatherArrivalTimeCalculator(
    private val clock: Clock = Clock.systemDefaultZone()
) : IWeatherArrivalTimeCalculator {
    override fun getArrivalTime(forecastedTime: Instant?, arrival: RelativeArrivalTime?): WeatherArrivalTime? {
        val now = Instant.now(clock)
        if (forecastedTime != null) {
            if (forecastedTime < now) {
                return WeatherArrivalTime.exact(now, forecastedTime)
            }
            val rounded = forecastedTime.toZonedDateTime().roundNearestMinute(15).toInstant()
            return WeatherArrivalTime.exact(now, rounded)
        }
        return arrival?.let { WeatherArrivalTime.fromRelative(now, it) }
    }
}
