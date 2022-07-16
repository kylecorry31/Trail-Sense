package com.kylecorry.trail_sense.weather.infrastructure

import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.weather.domain.PressureReading
import java.time.Instant

data class CurrentWeather(
    val prediction: WeatherPrediction,
    val pressureTendency: PressureTendency,
    val observation: WeatherObservation?
)

data class WeatherPrediction(val hourly: Weather, val daily: Weather)

// TODO: Expose sea level and barometric pressure
data class WeatherObservation(
    val time: Instant,
    val pressure: Pressure,
    val temperature: Temperature,
    val humidity: Float?
) {
    fun pressureReading(): PressureReading {
        return PressureReading(time, pressure.convertTo(PressureUnits.Hpa).pressure)
    }
}