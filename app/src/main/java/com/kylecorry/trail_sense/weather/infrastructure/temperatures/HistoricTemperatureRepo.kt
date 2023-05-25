package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import android.content.Context
import com.kylecorry.sol.math.Range
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

    override suspend fun getYearlyTemperatures(
        year: Int,
        location: Coordinate
    ): List<Pair<LocalDate, Range<Temperature>>> = onDefault {
        val monthly =
            HistoricMonthlyTemperatureRangeRepo.getMonthlyTemperatureRanges(context, location)

        getYearlyValues(year){
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
            daysBetween(start, it).toFloat()
        }

        val lows = lookupMonths.map {
            months[it.month]?.start?.temperature ?: 0f
        }

        val highs = lookupMonths.map {
            months[it.month]?.end?.temperature ?: 0f
        }

        val x = daysBetween(start, date).toFloat()

        val low = cubicInterpolation(
            x, xs[0], lows[0], xs[1], lows[1], xs[2], lows[2], xs[3], lows[3]
        )
        val high = cubicInterpolation(
            x, xs[0], highs[0], xs[1], highs[1], xs[2], highs[2], xs[3], highs[3]
        )

        Range(Temperature.celsius(min(low, high)), Temperature.celsius(max(low, high)))
    }

    // TODO: Extract these methods to Sol

    private inline fun <T> getYearlyValues(year: Int, valueProvider: (date: LocalDate) -> T): List<Pair<LocalDate, T>> {
        val values = mutableListOf<Pair<LocalDate, T>>()
        var date = LocalDate.of(year, Month.JANUARY, 1)

        while (date.year == year) {
            values.add(date to valueProvider(date))
            date = date.plusDays(1)
        }

        return values
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

    private fun daysBetween(start: LocalDate, end: LocalDate): Int {
        return Duration.between(start.atStartOfDay(), end.atStartOfDay()).toDays().toInt()
    }

    fun cubicInterpolation(
        x: Float,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float
    ): Float {
        val t = (x - x1) / (x2 - x1)
        val t2 = t * t
        val t3 = t2 * t

        val c0 = y1
        val c1 = 0.5f * (y2 - y0)
        val c2 = y0 - 2.5f * y1 + 2.0f * y2 - 0.5f * y3
        val c3 = 0.5f * (y3 - y0) + 1.5f * (y1 - y2)

        return c0 + c1 * t + c2 * t2 + c3 * t3
    }


}