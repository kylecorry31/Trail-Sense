package com.kylecorry.trail_sense.weather.infrastructure

import com.kylecorry.sol.science.meteorology.WeatherCondition

internal class PrimaryWeatherSelector {

    private val order = listOf(
        WeatherCondition.Storm,
        WeatherCondition.Precipitation,
        WeatherCondition.Wind,
        WeatherCondition.Overcast,
        WeatherCondition.Clear
    )

    fun getWeather(conditions: List<WeatherCondition>): WeatherCondition? {
        for (condition in order) {
            if (conditions.contains(condition)) {
                return condition
            }
        }
        return null
    }
}