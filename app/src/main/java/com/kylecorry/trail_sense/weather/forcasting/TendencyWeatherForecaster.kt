package com.kylecorry.trail_sense.weather.forcasting

import com.kylecorry.trail_sense.models.PressureTendency

class TendencyWeatherForecaster : IWeatherForecaster {
    override fun forcast(pressure: Float, tendency: PressureTendency): String {
        return when(tendency){
            PressureTendency.FALLING_SLOW -> "Worsening"
            PressureTendency.RISING_SLOW -> "Improving"
            PressureTendency.FALLING_FAST -> "Worsening soon"
            PressureTendency.RISING_FAST -> "Improving soon"
            PressureTendency.STEADY -> "No change"
        }
    }
}