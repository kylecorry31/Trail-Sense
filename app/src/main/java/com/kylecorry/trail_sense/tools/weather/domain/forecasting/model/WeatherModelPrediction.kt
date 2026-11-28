package com.kylecorry.trail_sense.tools.weather.domain.forecasting.model

import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.trail_sense.tools.weather.domain.ForecastCondition

internal data class WeatherModelPrediction(
    val conditions: List<ForecastCondition>,
    val laterConditions: List<WeatherCondition>
)
