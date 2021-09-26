package com.kylecorry.trail_sense.weather.domain.forcasting

import com.kylecorry.sol.science.meteorology.forecast.Weather
import com.kylecorry.trail_sense.weather.domain.PressureReading
import com.kylecorry.trail_sense.weather.domain.tendency.SlopePressureTendencyCalculator
import java.time.Duration

class DailyForecaster(private val slowThreshold: Float) : IWeatherForecaster {

    override fun forecast(readings: List<PressureReading>): Weather {

        val slope = SlopePressureTendencyCalculator().calculate(readings, Duration.ofHours(48)).amount

        return when {
            slope <= -slowThreshold -> Weather.WorseningSlow
            slope >= slowThreshold -> Weather.ImprovingSlow
            else -> Weather.Unknown
        }

    }
}