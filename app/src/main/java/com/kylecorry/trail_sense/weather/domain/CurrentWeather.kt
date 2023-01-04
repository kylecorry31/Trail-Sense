package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.meteorology.WeatherFront
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.database.Identifiable
import com.kylecorry.trail_sense.weather.domain.forecasting.PrimaryWeatherSelector
import com.kylecorry.trail_sense.weather.domain.forecasting.arrival.WeatherArrivalTime
import java.time.Instant

data class CurrentWeather(
    val prediction: WeatherPrediction,
    val pressureTendency: PressureTendency,
    val observation: WeatherObservation?,
    val clouds: Reading<CloudGenus?>?
)

enum class RelativeArrivalTime {
    Now,
    VerySoon,
    Soon,
    Later
}

data class TemperaturePrediction(
    val average: Temperature,
    val low: Temperature,
    val high: Temperature,
    val current: Temperature
)

data class WeatherPrediction(
    val hourly: List<WeatherCondition>,
    val daily: List<WeatherCondition>,
    val front: WeatherFront?,
    val hourlyArrival: WeatherArrivalTime?,
    val temperature: TemperaturePrediction?,
    val alerts: List<WeatherAlert>
) {

    private val primarySelector = PrimaryWeatherSelector()

    val primaryHourly = primarySelector.getWeather(hourly)
    val primaryDaily = primarySelector.getWeather(daily)
}

// TODO: Expose sea level and barometric pressure
data class WeatherObservation(
    override val id: Long,
    val time: Instant,
    val pressure: Pressure,
    val temperature: Temperature,
    val humidity: Float?
) : Identifiable {
    fun pressureReading(): Reading<Pressure> {
        return Reading(pressure, time)
    }
}