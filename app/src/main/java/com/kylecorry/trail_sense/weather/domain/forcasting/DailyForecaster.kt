package com.kylecorry.trail_sense.weather.domain.forcasting

import com.kylecorry.trail_sense.weather.domain.PressureReading
import com.kylecorry.trail_sense.weather.domain.tendency.SlopePressureTendencyCalculator
import org.threeten.bp.Duration

class DailyForecaster : IWeatherForecaster {

    override fun forecast(readings: List<PressureReading>): Weather {

        val slope = SlopePressureTendencyCalculator().calculate(readings, Duration.ofHours(48)).amount

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