package com.kylecorry.trail_sense.weather.infrastructure.subsystem

import com.kylecorry.andromeda.core.topics.ITopic
import com.kylecorry.trail_sense.weather.infrastructure.CurrentWeather
import com.kylecorry.trail_sense.weather.infrastructure.WeatherObservation

interface IWeatherSubsystem {
    val weatherChanged: ITopic

    suspend fun getWeather(): CurrentWeather
    suspend fun getHistory(): List<WeatherObservation>
    suspend fun invalidate()
}