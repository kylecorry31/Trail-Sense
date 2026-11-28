package com.kylecorry.trail_sense.tools.weather.infrastructure

import com.kylecorry.trail_sense.tools.weather.domain.ForecastSensitivity
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

interface IWeatherPreferences {
    val hasBarometer: Boolean
    var shouldMonitorWeather: Boolean
    val pressureSmoothing: Float
    var weatherUpdateFrequency: Duration
    val shouldShowDailyWeatherNotification: Boolean
    val shouldShowPressureInNotification: Boolean
    val shouldShowTemperatureInNotification: Boolean
    val useSeaLevelPressure: Boolean
    val seaLevelFactorInTemp: Boolean
    var barometerOffset: Float
    val pressureHistory: Duration
    val sendStormAlerts: Boolean
    val forecastSensitivity: ForecastSensitivity
    val stormSensitivity: ForecastSensitivity
    var dailyWeatherLastSent: LocalDate
    val dailyWeatherIsForTomorrow: Boolean
    var dailyForecastTime: LocalTime
    val leftButton: Int
    val rightButton: Int
    val showColoredNotificationIcon: Boolean
    val useAlarmForStormAlert: Boolean
}
