package com.kylecorry.trail_sense.tools.weather.domain

import com.kylecorry.sol.science.meteorology.WeatherCondition

/**
 * @param probability the chance from 0 to 1 that the condition happens by the end of its arrival window, or null when
 * it isn't from the forecast model
 * @param relativeProbability the probability divided by the probability the model needs to forecast the condition,
 * so 1 is just enough, or null when the model has no threshold for it, such as for the sky
 */
data class ForecastCondition(
    val condition: WeatherCondition,
    val probability: Float? = null,
    val arrival: RelativeArrivalTime? = null,
    val relativeProbability: Float? = null
)
