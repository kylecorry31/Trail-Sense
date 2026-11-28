package com.kylecorry.trail_sense.tools.weather.domain

import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.data.Identifiable
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.PrimaryWeatherSelector
import java.time.Duration
import java.time.Instant

data class CurrentWeather(
    val prediction: WeatherPrediction,
    val pressureTendency: PressureTendency,
    val observation: WeatherObservation?,
    val clouds: Reading<CloudGenus?>?
)

enum class RelativeArrivalTime {
    Soon,
    Later;

    companion object {
        val SOON_WINDOW: Duration = Duration.ofHours(3)
        val LATER_WINDOW: Duration = Duration.ofHours(12)
    }
}

data class TemperaturePrediction(
    val average: Temperature,
    val low: Temperature,
    val high: Temperature,
    val current: Temperature
)

data class WeatherPrediction(
    val hourly: List<ForecastCondition>,
    val daily: List<ForecastCondition>,
    val hourlyArrival: RelativeArrivalTime?,
    val temperature: TemperaturePrediction?,
    val alerts: List<WeatherAlert>
) {

    private val primarySelector = PrimaryWeatherSelector()

    val primaryHourly = primarySelector.getWeather(hourly)
    val primaryDaily = primarySelector.getWeather(daily)
}

data class WeatherObservation(
    override val id: Long,
    val time: Instant,
    val pressure: Pressure,
    val temperature: Temperature,
    val humidity: Float?,
    val location: Coordinate
) : Identifiable {
    fun pressureReading(): Reading<Pressure> {
        return Reading(pressure, time)
    }
}
