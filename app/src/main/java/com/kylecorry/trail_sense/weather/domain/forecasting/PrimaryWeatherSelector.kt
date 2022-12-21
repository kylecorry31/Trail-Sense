package com.kylecorry.trail_sense.weather.domain.forecasting

import com.kylecorry.sol.science.meteorology.WeatherCondition

internal class PrimaryWeatherSelector {

    private val order = listOf(
        WeatherCondition.Thunderstorm,
        WeatherCondition.Snow,
        WeatherCondition.Rain,
        WeatherCondition.Precipitation,
        WeatherCondition.Wind,
        WeatherCondition.Overcast,
        WeatherCondition.Clear
    )

    fun getWeather(conditions: List<WeatherCondition>): WeatherCondition? {
        return order.firstOrNull { conditions.contains(it) }
    }
}