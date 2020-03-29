package com.kylecorry.trail_sense.weather.forcasting

import com.kylecorry.trail_sense.shared.PressureReading
import com.kylecorry.trail_sense.weather.PressureTendencyCalculator
import java.time.Duration

class DailyForecaster : IWeatherForecaster {

    override fun forecast(readings: List<PressureReading>): Weather {

        val slope = PressureTendencyCalculator.getPressureTendency(readings, Duration.ofHours(48)).amount

        return when {
            slope <= -CHANGE_THRESHOLD -> Weather.WorseningSlow
            slope >= CHANGE_THRESHOLD -> Weather.ImprovingSlow
            else -> Weather.Unknown
        }

    }

    companion object {
        private const val CHANGE_THRESHOLD = 0.5f
    }

}