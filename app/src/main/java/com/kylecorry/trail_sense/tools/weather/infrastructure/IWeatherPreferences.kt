package com.kylecorry.trail_sense.tools.weather.infrastructure

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.forecast.ForecastSource
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
    val dailyForecastChangeThreshold: Float
    val hourlyForecastChangeThreshold: Float
    val stormAlertThreshold: Float
    var dailyWeatherLastSent: LocalDate
    val dailyWeatherIsForTomorrow: Boolean
    var dailyForecastTime: LocalTime
    val leftButton: Int
    val rightButton: Int
    val showColoredNotificationIcon: Boolean
    val forecastSource: ForecastSource
    val useAlarmForStormAlert: Boolean
}