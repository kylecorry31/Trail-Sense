package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import android.content.Context
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.interpolation.NewtonInterpolator
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.time.Time.daysUntil
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.extensions.getReadings
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.weather.infrastructure.temperatures.calculators.DailyTemperatureCalculator
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min

internal class HistoricTemperatureRepo(private val context: Context) : ITemperatureRepo {

    private val highInterpolator = NewtonInterpolator()
    private val lowInterpolator = NewtonInterpolator()

    override suspend fun getYearlyTemperatures(
        year: Int,
        location: Coordinate
    ): List<Pair<LocalDate, Range<Temperature>>> = onDefault {
        val monthly =
            HistoricMonthlyTemperatureRangeRepo.getMonthlyTemperatureRanges(context, location)

        Time.getYearlyValues(year) {
            getDailyRange(location, it, monthly)
        }
    }

    override suspend fun getTemperatures(
        location: Coordinate,
        start: ZonedDateTime,
        end: ZonedDateTime
    ): List<Reading<Temperature>> {
        val calculator = DailyTemperatureCalculator(location, this::getDailyRange)

        return getReadings(start, end, Duration.ofMinutes(10)) {
            calculator.calculate(it)
        }
    }

    override suspend fun getTemperature(location: Coordinate, time: ZonedDateTime): Temperature {
        val calculator = DailyTemperatureCalculator(location, this::getDailyRange)
        return calculator.calculate(time)
    }

    override suspend fun getDailyTemperatureRange(
        location: Coordinate,
        date: LocalDate
    ): Range<Temperature> {
        return getDailyRange(location, date)
    }

    private suspend fun getDailyRange(
        location: Coordinate,
        date: LocalDate,
        monthlyRanges: Map<Month, Range<Temperature>>? = null
    ): Range<Temperature> = onDefault {
        val months =
            monthlyRanges ?: HistoricMonthlyTemperatureRangeRepo.getMonthlyTemperatureRanges(
                context, location
            )

        val lookupMonths = getSurroundingMonths(date)

        val start = lookupMonths.first()

        val xs = lookupMonths.map {
            start.daysUntil(it).toFloat()
        }

        val lows = lookupMonths.map {
            months[it.month]?.start?.temperature ?: 0f
        }

        val highs = lookupMonths.map {
            months[it.month]?.end?.temperature ?: 0f
        }

        val x = start.daysUntil(date).toFloat()

        val low = highInterpolator.interpolate(x, xs, lows)
        val high = lowInterpolator.interpolate(x, xs, highs)

        Range(Temperature.celsius(min(low, high)), Temperature.celsius(max(low, high)))
    }

    private fun getSurroundingMonths(date: LocalDate): List<LocalDate> {
        val midMonth = LocalDate.of(date.year, date.month, 15)
        return if (date > midMonth) {
            listOf(
                midMonth.minusMonths(1),
                midMonth,
                // The date is between these
                midMonth.plusMonths(1),
                midMonth.plusMonths(2)
            )
        } else {
            listOf(
                midMonth.minusMonths(2),
                midMonth.minusMonths(1),
                // The date is between these
                midMonth,
                midMonth.plusMonths(1)
            )
        }
    }

}