package com.kylecorry.trail_sense.weather.domain.forecasting.alerts

import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.trail_sense.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.weather.domain.WeatherAlert
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem

internal class WeatherAlertGenerator : IWeatherAlertGenerator {
    override fun getAlerts(weather: CurrentWeather): List<WeatherAlert> {
        val alerts = mutableListOf<WeatherAlert>()

        if (weather.prediction.hourly.contains(WeatherCondition.Storm)) {
            listOf(WeatherAlert.Storm)
        }

        alerts.addAll(getTemperatureAlerts(weather))

        return alerts
    }

    private fun getTemperatureAlerts(weather: CurrentWeather): List<WeatherAlert> {
        weather.prediction.temperature ?: return emptyList()
        return if (weather.prediction.temperature.low.celsius().temperature <= WeatherSubsystem.COLD) {
            listOf(WeatherAlert.Cold)
        } else if (weather.prediction.temperature.high.celsius().temperature >= WeatherSubsystem.HOT) {
            listOf(WeatherAlert.Hot)
        } else {
            emptyList()
        }
    }
}