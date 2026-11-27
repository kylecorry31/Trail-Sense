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
        if (forecastedTime != null) {
            if (forecastedTime < Instant.now(clock)) {
                return WeatherArrivalTime(forecastedTime, true)
            }
            val rounded = forecastedTime.toZonedDateTime().roundNearestMinute(15).toInstant()
            return WeatherArrivalTime(rounded, true)
        }
        return arrival?.let { WeatherArrivalTime.fromRelative(Instant.now(clock), it) }
    }
}
