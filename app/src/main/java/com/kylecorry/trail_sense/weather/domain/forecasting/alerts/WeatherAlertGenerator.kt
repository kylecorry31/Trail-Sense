package com.kylecorry.trail_sense.weather.domain.forecasting.alerts

import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.trail_sense.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.weather.domain.WeatherAlert
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem

internal class WeatherAlertGenerator : IWeatherAlertGenerator {
    override fun getAlerts(weather: CurrentWeather): List<WeatherAlert> {
        val alerts = mutableListOf<WeatherAlert>()

        if (weather.prediction.hourly.contains(WeatherCondition.Storm)) {
            alerts.add(WeatherAlert.Storm)
        }

        alerts.addAll(getTemperatureAlerts(weather))

        return alerts
    }

    private fun getTemperatureAlerts(weather: CurrentWeather): List<WeatherAlert> {
        weather.prediction.temperature ?: return emptyList()

        val alerts = mutableListOf<WeatherAlert>()

        if (weather.prediction.temperature.low.celsius().temperature <= WeatherSubsystem.COLD) {
            alerts.add(WeatherAlert.Cold)
        }

        if (weather.prediction.temperature.high.celsius().temperature >= WeatherSubsystem.HOT) {
            alerts.add(WeatherAlert.Hot)
        }

        return alerts
    }
}