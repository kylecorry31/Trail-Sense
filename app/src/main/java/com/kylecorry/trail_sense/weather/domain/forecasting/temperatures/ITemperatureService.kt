package com.kylecorry.trail_sense.weather.domain.forecasting.temperatures

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import java.time.LocalDate
import java.time.ZonedDateTime

internal interface ITemperatureService {
    suspend fun getTemperature(time: ZonedDateTime): Temperature
    suspend fun getTemperatures(start: ZonedDateTime, end: ZonedDateTime): List<Reading<Temperature>>
    suspend fun getTemperatureRange(date: LocalDate): Range<Temperature>
    suspend fun getTemperatureRanges(year: Int): List<Pair<LocalDate, Range<Temperature>>>
    suspend fun getTemperatureRange(start: ZonedDateTime, end: ZonedDateTime): Range<Temperature>
}