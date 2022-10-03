package com.kylecorry.trail_sense.weather.infrastructure.subsystem

import com.kylecorry.andromeda.core.topics.ITopic
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.weather.infrastructure.CurrentWeather
import com.kylecorry.trail_sense.weather.infrastructure.WeatherObservation
import java.time.Duration

// TODO: Split into two subsystems: Weather and Weather Monitor
interface IWeatherSubsystem {
    val weatherChanged: ITopic

    // Weather monitor
    val weatherMonitorState: com.kylecorry.andromeda.core.topics.generic.ITopic<FeatureState>
    val weatherMonitorFrequency: com.kylecorry.andromeda.core.topics.generic.ITopic<Duration>

    suspend fun getWeather(): CurrentWeather
    suspend fun getHistory(): List<WeatherObservation>
    suspend fun getCloudHistory(): List<Reading<CloudGenus?>>
    suspend fun getRawHistory(): List<Reading<RawWeatherObservation>>

    // Weather monitor
    fun enableMonitor()
    fun disableMonitor()
    suspend fun updateWeather(background: Boolean)
}