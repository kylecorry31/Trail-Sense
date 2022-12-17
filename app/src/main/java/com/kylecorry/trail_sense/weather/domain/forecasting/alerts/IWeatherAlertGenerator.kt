package com.kylecorry.trail_sense.weather.domain.forecasting.alerts

import com.kylecorry.trail_sense.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.weather.domain.WeatherAlert

internal interface IWeatherAlertGenerator {
    fun getAlerts(weather: CurrentWeather): List<WeatherAlert>
}