package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import java.time.LocalDate
import java.time.ZonedDateTime

interface ITemperatureRepo {
    suspend fun getYearlyTemperatures(
        year: Int,
        location: Coordinate
    ): List<Pair<LocalDate, Range<Temperature>>>

    suspend fun getTemperatures(
        location: Coordinate,
        start: ZonedDateTime,
        end: ZonedDateTime
    ): List<Reading<Temperature>>

    suspend fun getTemperature(location: Coordinate, time: ZonedDateTime): Temperature
    suspend fun getDailyTemperatureRange(location: Coordinate, date: LocalDate): Range<Temperature>
}