package com.kylecorry.trail_sense.tools.weather.domain

import com.kylecorry.sol.science.meteorology.WeatherCondition

data class ForecastCondition(val condition: WeatherCondition, val probability: Float? = null)
