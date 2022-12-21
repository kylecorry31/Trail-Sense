package com.kylecorry.trail_sense.weather.infrastructure.subsystem

import com.kylecorry.andromeda.core.topics.ITopic
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.weather.domain.WeatherObservation
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

// TODO: Split into two subsystems: Weather and Weather Monitor
interface IWeatherSubsystem {
    val weatherChanged: ITopic

    // Weather monitor
    val weatherMonitorState: com.kylecorry.andromeda.core.topics.generic.ITopic<FeatureState>
    val weatherMonitorFrequency: com.kylecorry.andromeda.core.topics.generic.ITopic<Duration>
    fun getWeatherMonitorState(): FeatureState
    fun getWeatherMonitorFrequency(): Duration

    suspend fun getWeather(): CurrentWeather
    suspend fun getHistory(): List<WeatherObservation>
    suspend fun getTemperature(
        time: ZonedDateTime,
        location: Coordinate? = null,
        elevation: Distance? = null,
        calibrated: Boolean = true
    ): Reading<Temperature>

    suspend fun getTemperatures(
        start: ZonedDateTime,
        end: ZonedDateTime,
        location: Coordinate? = null,
        elevation: Distance? = null,
        calibrated: Boolean = true
    ): List<Reading<Temperature>>

    suspend fun getTemperatureRange(
        date: LocalDate,
        location: Coordinate? = null,
        elevation: Distance? = null,
        calibrated: Boolean = true
    ): Range<Temperature>

    suspend fun getTemperatureRanges(
        year: Int,
        location: Coordinate? = null,
        elevation: Distance? = null,
        calibrated: Boolean = true
    ): List<Pair<LocalDate, Range<Temperature>>>

    suspend fun getCloudHistory(): List<Reading<CloudGenus?>>
    suspend fun getRawHistory(): List<Reading<RawWeatherObservation>>

    // Weather monitor
    fun enableMonitor()
    fun disableMonitor()
    suspend fun updateWeather(background: Boolean)
}