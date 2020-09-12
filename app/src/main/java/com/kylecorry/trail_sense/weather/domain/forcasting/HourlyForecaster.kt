package com.kylecorry.trail_sense.weather.domain.forcasting

import com.kylecorry.trailsensecore.domain.weather.PressureReading
import com.kylecorry.trailsensecore.domain.weather.Weather
import com.kylecorry.trailsensecore.domain.weather.WeatherService
import java.time.Duration
import java.time.Instant
import kotlin.math.absoluteValue

class HourlyForecaster(private val stormThreshold: Float, private val changeThreshold: Float) : IWeatherForecaster {

    private val service = WeatherService()

    override fun forecast(readings: List<PressureReading>): Weather {

        val currentReading = readings.lastOrNull()
        val prevReading = readings.minByOrNull { Duration.between(it.time, Instant.now().minus(Duration.ofHours(3))).seconds.absoluteValue }

        if (currentReading != null && prevReading != null){
            val tendency = service.getTendency(prevReading, currentReading, changeThreshold)
            return service.forecast(tendency, currentReading, stormThreshold)
        }

        return Weather.NoChange
    }
}