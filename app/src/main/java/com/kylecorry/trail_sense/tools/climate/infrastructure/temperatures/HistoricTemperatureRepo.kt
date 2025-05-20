package com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.tools.climate.infrastructure.MonthlyValueInterpolator
import com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.calculators.DailyTemperatureCalculator
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min

internal class HistoricTemperatureRepo(private val context: Context) : ITemperatureRepo {

    private val highInterpolator = MonthlyValueInterpolator()
    private val lowInterpolator = MonthlyValueInterpolator()

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

        return Time.getReadings(start, end, Duration.ofMinutes(10)) {
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

        val lowMonths = months.mapValues {
            it.value.start.celsius().temperature
        }

        val highMonths = months.mapValues {
            it.value.end.celsius().temperature
        }

        val low = lowInterpolator.interpolate(date, lowMonths)
        val high = highInterpolator.interpolate(date, highMonths)

        Range(Temperature.celsius(min(low, high)), Temperature.celsius(max(low, high)))
    }
}