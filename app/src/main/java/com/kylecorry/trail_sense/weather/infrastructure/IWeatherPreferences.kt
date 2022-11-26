package com.kylecorry.trail_sense.weather.infrastructure

import com.kylecorry.trail_sense.shared.QuickActionType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

interface IWeatherPreferences {
    val hasBarometer: Boolean
    var shouldMonitorWeather: Boolean
    val pressureSmoothing: Float
    var weatherUpdateFrequency: Duration
    val shouldShowDailyWeatherNotification: Boolean
    val shouldShowWeatherNotification: Boolean
    val shouldShowPressureInNotification: Boolean
    val useSeaLevelPressure: Boolean
    val seaLevelFactorInTemp: Boolean
    val pressureHistory: Duration
    val sendStormAlerts: Boolean
    val dailyForecastChangeThreshold: Float
    val hourlyForecastChangeThreshold: Float
    val stormAlertThreshold: Float
    var minBatteryTemperature: Float
    var minActualTemperature: Float
    var maxBatteryTemperature: Float
    var maxActualTemperature: Float
    var minBatteryTemperatureF: Float
    var minActualTemperatureF: Float
    var maxBatteryTemperatureF: Float
    var maxActualTemperatureF: Float
    var dailyWeatherLastSent: LocalDate
    val dailyWeatherIsForTomorrow: Boolean
    var dailyForecastTime: LocalTime
    val leftButton: QuickActionType
    val rightButton: QuickActionType
    val showColoredNotificationIcon: Boolean
}