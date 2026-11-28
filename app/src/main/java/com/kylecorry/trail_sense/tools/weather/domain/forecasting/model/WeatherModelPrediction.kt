package com.kylecorry.trail_sense.tools.weather.domain.forecasting.model

import com.kylecorry.trail_sense.tools.weather.domain.ForecastCondition

/**
 * @param snowChance the chance from 0 to 1 that precipitation in the next 12 hours is mostly snow or ice
 * @param laterSnowChance the same for precipitation in 12 to 24 hours
 */
internal data class WeatherModelPrediction(
    val conditions: List<ForecastCondition>,
    val laterConditions: List<ForecastCondition>,
    val snowChance: Float,
    val laterSnowChance: Float,
    val isPrecipitationSnow: Boolean,
    val isLaterPrecipitationSnow: Boolean,
    val isStormAlert: Boolean
)
