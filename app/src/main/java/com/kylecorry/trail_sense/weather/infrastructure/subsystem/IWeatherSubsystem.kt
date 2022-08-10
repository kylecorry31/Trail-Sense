package com.kylecorry.trail_sense.weather.infrastructure.subsystem

import com.kylecorry.andromeda.core.topics.ITopic
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.weather.infrastructure.CurrentWeather
import com.kylecorry.trail_sense.weather.infrastructure.WeatherObservation
import java.time.Duration

// TODO: Split into two subsystems: Weather and Weather Monitor
interface IWeatherSubsystem {
    val weatherChanged: ITopic

    // Weather monitor
    val weatherMonitorStateChanged: com.kylecorry.andromeda.core.topics.generic.ITopic<FeatureState>
    val weatherMonitorFrequencyChanged: com.kylecorry.andromeda.core.topics.generic.ITopic<Duration>

    suspend fun getWeather(): CurrentWeather
    suspend fun getHistory(): List<WeatherObservation>

    // Weather monitor
    fun enableMonitor()
    fun disableMonitor()
    suspend fun updateWeather(background: Boolean)
}