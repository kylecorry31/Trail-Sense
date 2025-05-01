package com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem

import com.kylecorry.andromeda.core.topics.ITopic
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.KoppenGeigerClimateClassification
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import java.time.LocalDate
import java.time.Month
import java.time.ZonedDateTime

// TODO: Split into two subsystems: Weather and Weather Monitor
interface IWeatherSubsystem {
    val weatherChanged: ITopic

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
    suspend fun getRawHistory(applyPressureOffset: Boolean = false): List<Reading<RawWeatherObservation>>

    suspend fun getMonthlyPrecipitation(location: Coordinate? = null): Map<Month, Distance>

    suspend fun getClimateClassification(
        location: Coordinate? = null,
        elevation: Distance? = null,
        calibrated: Boolean = true
    ): KoppenGeigerClimateClassification

    suspend fun updateWeather()
}