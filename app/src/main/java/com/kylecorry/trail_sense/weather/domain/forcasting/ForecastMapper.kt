package com.kylecorry.trail_sense.weather.domain.forcasting

import com.kylecorry.sol.science.meteorology.forecast.Weather

class ForecastMapper {

    fun map(forecast: Float): Weather {
        return when {
            forecast == -1f -> {
                Weather.Storm
            }
            forecast <= -0.5f -> {
                Weather.WorseningFast
            }
            forecast <= -0.1f -> {
                Weather.WorseningSlow
            }
            forecast < 1f -> {
                Weather.NoChange
            }
            forecast < 0.5f -> {
                Weather.ImprovingSlow
            }
            else -> {
                Weather.ImprovingFast
            }
        }
    }

}