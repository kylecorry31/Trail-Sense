package com.kylecorry.trail_sense.weather.forcasting

import com.kylecorry.trail_sense.models.PressureTendency

class PressureAndTendencyWeatherForecaster : IWeatherForecaster {
    override fun forcast(pressure: Float, tendency: PressureTendency): String {
        return when {
            isHigh(pressure) -> {
                when (tendency){
                    PressureTendency.FALLING_FAST -> "Cloudy"
                    PressureTendency.FALLING_SLOW -> "Fair"
                    else -> "Fair"
                }
            }
            isLow(pressure) -> {
                when (tendency){
                    PressureTendency.FALLING_FAST -> "Stormy"
                    PressureTendency.FALLING_SLOW -> "Precipitation"
                    else -> "Improving conditions"
                }
            }
            else -> {
                when (tendency){
                    PressureTendency.FALLING_FAST -> "Precipitation"
                    PressureTendency.FALLING_SLOW -> "Little change"
                   else -> "No change"
                }
            }
        }
    }

    private fun isHigh(pressure: Float): Boolean {
        return pressure >= 1022.68f
    }

    private fun isLow(pressure: Float): Boolean {
        return pressure <= 1009.14f
    }
}