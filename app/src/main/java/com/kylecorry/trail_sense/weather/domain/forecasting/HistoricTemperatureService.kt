package com.kylecorry.trail_sense.weather.domain.forecasting

import android.content.Context
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.shared.extensions.range
import com.kylecorry.trail_sense.weather.infrastructure.temperatures.TemperatureEstimator
import java.time.LocalDate
import java.time.ZonedDateTime

internal class HistoricTemperatureService(
    context: Context,
    private val location: Coordinate,
    private val elevation: Distance = Distance.meters(0f)
) : ITemperatureService {

    private val estimator = TemperatureEstimator(context)

    override suspend fun getTemperature(time: ZonedDateTime): Temperature = onDefault {
        val temperature = estimator.getTemperature(location, time)
        Meteorology.getTemperatureAtElevation(
            temperature,
            Distance.meters(0f),
            elevation
        )
    }

    override suspend fun getTemperatures(
        start: ZonedDateTime,
        end: ZonedDateTime
    ): List<Reading<Temperature>> = onDefault {
        val temperatures = estimator.getTemperatures(start, end, location)
        temperatures.map {
            it.copy(
                value = Meteorology.getTemperatureAtElevation(
                    it.value,
                    Distance.meters(0f),
                    elevation
                )
            )
        }
    }

    override suspend fun getTemperatureRange(date: LocalDate): Range<Temperature> = onDefault {
        val temperatures = estimator.getDailyTemperatureRange(location, date)
        Range(
            Meteorology.getTemperatureAtElevation(
                temperatures.start,
                Distance.meters(0f),
                elevation
            ),
            Meteorology.getTemperatureAtElevation(
                temperatures.end,
                Distance.meters(0f),
                elevation
            )
        )
    }

    override suspend fun getTemperatureRange(
        start: ZonedDateTime,
        end: ZonedDateTime
    ): Range<Temperature> = onDefault {
        val forecast = getTemperatures(start, end)
        forecast.map { it.value }.range()!!
    }

    override suspend fun getTemperatureRanges(year: Int): List<Pair<LocalDate, Range<Temperature>>> =
        onDefault {
            val temperatures = estimator.getYearlyTemperatures(year, location)
            temperatures.map {
                it.first to Range(
                    Meteorology.getTemperatureAtElevation(
                        it.second.start,
                        Distance.meters(0f),
                        elevation
                    ),
                    Meteorology.getTemperatureAtElevation(
                        it.second.end,
                        Distance.meters(0f),
                        elevation
                    )
                )
            }
        }
}