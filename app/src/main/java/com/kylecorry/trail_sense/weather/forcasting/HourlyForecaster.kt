package com.kylecorry.trail_sense.weather.forcasting

import com.kylecorry.trail_sense.models.PressureReading
import com.kylecorry.trail_sense.models.PressureTendency
import com.kylecorry.trail_sense.weather.PressureTendencyCalculator
import java.time.Duration

class HourlyForecaster : IWeatherForecaster {

    override fun forecast(readings: List<PressureReading>): Weather {
        return when(PressureTendencyCalculator.getPressureTendency(readings, Duration.ofHours(3).plusMinutes(5))){
            PressureTendency.FALLING_SLOW -> Weather.WorseningSlow
            PressureTendency.RISING_SLOW -> Weather.ImprovingSlow
            PressureTendency.FALLING_FAST -> Weather.WorseningFast
            PressureTendency.RISING_FAST -> Weather.ImprovingFast
            PressureTendency.STEADY -> Weather.NoChange
        }

    }
}