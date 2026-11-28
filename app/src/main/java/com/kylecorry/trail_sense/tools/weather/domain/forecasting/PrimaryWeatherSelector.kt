package com.kylecorry.trail_sense.tools.weather.domain.forecasting

import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.trail_sense.tools.weather.domain.ForecastCondition

internal class PrimaryWeatherSelector {

    fun getWeather(conditions: List<ForecastCondition>): WeatherCondition? {
        var primary: WeatherCondition? = null
        var primaryScore = 0f
        for (condition in IMPORTANCE.keys) {
            val forecast = conditions.firstOrNull { it.condition == condition } ?: continue
            val score = (forecast.relativeProbability ?: 1f) * IMPORTANCE.getValue(condition)
            if (primary == null || score > primaryScore) {
                primary = condition
                primaryScore = score
            }
        }
        return primary
    }

    companion object {
        private val IMPORTANCE = linkedMapOf(
            WeatherCondition.Thunderstorm to 3f,
            WeatherCondition.Storm to 3f,
            WeatherCondition.Snow to 2f,
            WeatherCondition.Rain to 2f,
            WeatherCondition.Precipitation to 2f,
            WeatherCondition.Wind to 1.5f,
            WeatherCondition.Overcast to 1f,
            WeatherCondition.Clear to 1f
        )
    }
}
