package com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival

import com.kylecorry.trail_sense.tools.weather.domain.RelativeArrivalTime
import java.time.Instant

internal interface IWeatherArrivalTimeCalculator {
    fun getArrivalTime(forecastedTime: Instant?, arrival: RelativeArrivalTime?): WeatherArrivalTime?
}
