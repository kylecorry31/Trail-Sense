package com.kylecorry.trail_sense.plugin.sample.domain

import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.trail_sense.shared.ProguardIgnore
import java.time.Instant
import java.time.LocalDate

data class Forecast(
    val time: Instant,
    val elevation: Float?,
    val citation: String?,
    val current: CurrentWeather,
    val hourly: List<HourlyWeather>,
    val daily: List<DailyWeather>
): ProguardIgnore

data class CurrentWeather(
    val time: Instant,
    val weather: WeatherCondition?,
    val temperature: Float?,
    val humidity: Float?,
    val windSpeed: Float?
): ProguardIgnore

data class HourlyWeather(
    val time: Instant,
    val weather: WeatherCondition?,
    val temperature: Float?,
    val humidity: Float?,
    val windSpeed: Float?,
    val precipitationChance: Float?,
    val rainAmount: Float?,
    val snowAmount: Float?
): ProguardIgnore

// TODO: Is this needed or can Trail Sense figure this out from the hourly data?
data class DailyWeather(
    val date: LocalDate,
    val weather: WeatherCondition?,
    val lowTemperature: Float?,
    val highTemperature: Float?,
    val precipitationChance: Float?,
    val rainAmount: Float?,
    val snowAmount: Float?
): ProguardIgnore