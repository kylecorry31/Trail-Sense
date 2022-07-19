package com.kylecorry.trail_sense.weather.domain.forcasting

import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.weather.domain.tendency.SlopePressureTendencyCalculator
import java.time.Duration

class DailyForecaster(private val slowThreshold: Float) : IWeatherForecaster {

    override fun forecast(readings: List<Reading<Pressure>>): Weather {

        val slope = SlopePressureTendencyCalculator().calculate(readings, Duration.ofHours(48)).amount

        return when {
            slope <= -slowThreshold -> Weather.WorseningSlow
            slope >= slowThreshold -> Weather.ImprovingSlow
            else -> Weather.Unknown
        }

    }
}