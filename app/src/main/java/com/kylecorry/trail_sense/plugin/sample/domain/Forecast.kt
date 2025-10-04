package com.kylecorry.trail_sense.plugin.sample.domain

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.ProguardIgnore
import java.time.Instant
import java.time.LocalDate

data class Forecast(
    val current: HourlyWeather,
    val hourly: List<HourlyWeather>,
    val daily: List<DailyWeather>
): ProguardIgnore

data class HourlyWeather(
    val time: Instant,
    val temperature: Temperature,
    val feelsLikeTemperature: Temperature,
    val humidity: Float,
    val precipitationProbability: Float,
    val precipitation: Distance,
    val rain: Distance,
    val showers: Distance,
    val snow: Distance,
    val snowDepth: Distance,
    val weatherCode: Int,
    val cloudCover: Float,
    val visibility: Distance,
    val windSpeed: Speed,
    val windGusts: Speed,
    val uvIndex: Float
): ProguardIgnore

data class DailyWeather(
    val date: LocalDate,
    val weatherCode: Int,
    val maxTemperature: Temperature,
    val minTemperature: Temperature,
    val maxWindSpeed: Speed,
    val maxWindGusts: Speed,
    val snowfall: Distance,
    val showers: Distance,
    val rain: Distance,
    val uvIndex: Float
): ProguardIgnore