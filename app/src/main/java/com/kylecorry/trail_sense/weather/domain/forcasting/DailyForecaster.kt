package com.kylecorry.trail_sense.weather.domain.forcasting

import com.kylecorry.trail_sense.weather.domain.tendency.SlopePressureTendencyCalculator
import com.kylecorry.trailsensecore.domain.weather.PressureReading
import com.kylecorry.trailsensecore.domain.weather.Weather
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