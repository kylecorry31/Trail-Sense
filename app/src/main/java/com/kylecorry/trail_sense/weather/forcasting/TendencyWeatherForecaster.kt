package com.kylecorry.trail_sense.weather.forcasting

import com.kylecorry.trail_sense.models.PressureTendency

class TendencyWeatherForecaster : IWeatherForecaster {
    override fun forcast(pressure: Float, tendency: PressureTendency): String {
        return when(tendency){
            PressureTendency.FALLING_SLOW -> "Weather may worsen"
            PressureTendency.RISING_SLOW -> "Weather may improve"
            PressureTendency.FALLING_FAST -> "Weather will worsen soon"
            PressureTendency.RISING_FAST -> "Weather will improve soon "
            PressureTendency.STEADY -> "Weather not changing"
        }
    }
}