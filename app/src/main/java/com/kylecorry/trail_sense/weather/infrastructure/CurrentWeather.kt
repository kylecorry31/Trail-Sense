package com.kylecorry.trail_sense.weather.infrastructure

import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import java.time.Instant

data class CurrentWeather(
    val prediction: WeatherPrediction,
    val pressureTendency: PressureTendency,
    val observation: WeatherObservation?,
    val clouds: Reading<CloudGenus?>?
)

data class WeatherPrediction(val hourly: Weather, val daily: Weather)

// TODO: Expose sea level and barometric pressure
data class WeatherObservation(
    val time: Instant,
    val pressure: Pressure,
    val temperature: Temperature,
    val humidity: Float?
) {
    fun pressureReading(): Reading<Pressure> {
        return Reading(pressure, time)
    }
}