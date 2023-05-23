package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import android.content.Context
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.extensions.getReadings
import com.kylecorry.trail_sense.weather.infrastructure.temperatures.calculators.DailyTemperatureCalculator
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.time.ZonedDateTime

internal class HistoricTemperatureRepo(private val context: Context) : ITemperatureRepo {

    override suspend fun getYearlyTemperatures(
        year: Int,
        location: Coordinate
    ): List<Pair<LocalDate, Range<Temperature>>> {
        val byMonth = Month.values().map {
            val date = LocalDate.of(year, it, 15)
            date to getDailyTemperatureRange(location, date)
        }

        var date = LocalDate.of(year, Month.JANUARY, 1)

        val readings = mutableListOf<Pair<LocalDate, Range<Temperature>>>()

        while (date.year == year) {

            if (date.dayOfMonth == 15) {
                readings.add(byMonth[date.monthValue - 1])
                date = date.plusDays(1)
                continue
            }

            val startDate = if (date.dayOfMonth > 15) {
                date.withDayOfMonth(15)
            } else {
                date.withDayOfMonth(15).minusMonths(1)
            }

            val endDate = if (date.dayOfMonth > 15) {
                date.withDayOfMonth(15).plusMonths(1)
            } else {
                date.withDayOfMonth(15)
            }

            val start = byMonth[startDate.monthValue - 1]
            val end = byMonth[endDate.monthValue - 1]

            val daysSinceStart =
                Duration.between(startDate.atStartOfDay(), date.atStartOfDay()).toDays()
            val daysBetweenMonths =
                Duration.between(startDate.atStartOfDay(), endDate.atStartOfDay()).toDays()

            val pct = daysSinceStart / daysBetweenMonths.toFloat()

            readings.add(date to lerp(pct, start.second, end.second))

            date = date.plusDays(1)
        }

        return readings
    }

    override suspend fun getTemperatures(
        location: Coordinate,
        start: ZonedDateTime,
        end: ZonedDateTime
    ): List<Reading<Temperature>> {
        val calculator = DailyTemperatureCalculator(location) { location, date ->
            getDailyTemperatureRange(location, date)
        }

        return getReadings(
            start,
            end,
            Duration.ofMinutes(10)
        ) {
            calculator.calculate(it)
        }
    }

    override suspend fun getTemperature(location: Coordinate, time: ZonedDateTime): Temperature {
        val calculator = DailyTemperatureCalculator(location) { location, date ->
            getDailyTemperatureRange(location, date)
        }
        return calculator.calculate(time)
    }

    override suspend fun getDailyTemperatureRange(
        location: Coordinate,
        date: LocalDate
    ): Range<Temperature> {
        return if (date.dayOfMonth == 15) {
            getMonthlyTemp(location, date)
        } else if (date.dayOfMonth > 15) {
            // TODO: Expose a method on the HistoricMonthlyTemperatureRangeRepo to get multiple months / full year
            // Load this month and next
            val thisMonth = getMonthlyTemp(
                location,
                date
            )
            val nextMonth = getMonthlyTemp(
                location,
                date.plusMonths(1)
            )
            val daysSinceMiddle = Duration.between(
                date.withDayOfMonth(15).atStartOfDay(),
                date.atStartOfDay()
            )
            val daysBetweenMonths = Duration.between(
                date.withDayOfMonth(15).atStartOfDay(),
                date.plusMonths(1).withDayOfMonth(15).atStartOfDay()
            )
            val pct = daysSinceMiddle.toDays() / daysBetweenMonths.toDays().toDouble()
            lerp(pct.toFloat(), thisMonth, nextMonth)
        } else {
            // Load this month and previous
            val thisMonth = getMonthlyTemp(
                location,
                date
            )
            val prevMonth = getMonthlyTemp(
                location,
                date.minusMonths(1)
            )
            val daysSinceMiddle = Duration.between(
                date.minusMonths(1).withDayOfMonth(15).atStartOfDay(),
                date.atStartOfDay()
            )
            val daysBetweenMonths = Duration.between(
                date.minusMonths(1).withDayOfMonth(15).atStartOfDay(),
                date.withDayOfMonth(15).atStartOfDay()
            )
            val pct = daysSinceMiddle.toDays() / daysBetweenMonths.toDays().toDouble()
            lerp(pct.toFloat(), prevMonth, thisMonth)
        }
    }

    private suspend fun getMonthlyTemp(
        location: Coordinate,
        date: LocalDate
    ): Range<Temperature> {
        return HistoricMonthlyTemperatureRangeRepo.getMonthlyTemperatureRange(
            context,
            location,
            date.month
        )
    }

    private fun lerp(
        pct: Float,
        lower: Range<Temperature>,
        upper: Range<Temperature>
    ): Range<Temperature> {
        val lerpedLow = SolMath.lerp(pct, lower.start.temperature, upper.start.temperature)
        val lerpedHigh = SolMath.lerp(pct, lower.end.temperature, upper.end.temperature)
        return Range(Temperature.celsius(lerpedLow), Temperature.celsius(lerpedHigh))
    }

}