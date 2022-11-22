package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import android.content.Context
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.time.Time.atEndOfDay
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.extensions.getReadings
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.ceil
import kotlin.math.floor

internal class TemperatureEstimator(private val context: Context) {

    fun getTemperaturesForDay(location: Coordinate, date: LocalDate): List<Reading<Temperature>> {
        val calculator = TemperatureCalculator(context, location, date)
        return getReadings(
            date.atStartOfDay().toZonedDateTime(),
            date.atEndOfDay().toZonedDateTime(),
            Duration.ofMinutes(10)
        ) {
            calculator.getTemperature(it)
        }
    }

    fun getTemperature(location: Coordinate, time: ZonedDateTime): Temperature {
        val calculator = TemperatureCalculator(context, location, time.toLocalDate())
        return calculator.getTemperature(time)
    }

    fun getDailyTemperatureRange(location: Coordinate, date: LocalDate): Range<Temperature> {
        return if (date.dayOfMonth == 15) {
            getMonthlyTemperature(location, date)
        } else if (date.dayOfMonth > 15) {
            // Load this month and next
            val thisMonth = getMonthlyTemperature(
                location,
                date
            )
            val nextMonth = getMonthlyTemperature(
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
            val lerpedLow = SolMath.lerp(pct.toFloat(), thisMonth.start.temperature, nextMonth.start.temperature)
            val lerpedHigh = SolMath.lerp(pct.toFloat(), thisMonth.end.temperature, nextMonth.end.temperature)
            Range(Temperature.celsius(lerpedLow), Temperature.celsius(lerpedHigh))
        } else {
            // Load this month and previous
            val thisMonth = getMonthlyTemperature(
                location,
                date
            )
            val prevMonth = getMonthlyTemperature(
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
            val lerpedLow = SolMath.lerp(pct.toFloat(), prevMonth.start.temperature, thisMonth.start.temperature)
            val lerpedHigh = SolMath.lerp(pct.toFloat(), prevMonth.end.temperature, thisMonth.end.temperature)
            Range(Temperature.celsius(lerpedLow), Temperature.celsius(lerpedHigh))
        }
    }

    private fun getMonthlyTemperature(location: Coordinate, date: LocalDate): Range<Temperature> {
        val start = floor(location.latitude)
        val end = ceil(location.latitude)

        if (start == end) {
            return HistoricTemperatureLookup.getMonthlyTemperatureRange(
                context,
                location,
                date.month
            )
        }

        val lower = HistoricTemperatureLookup.getMonthlyTemperatureRange(
            context,
            location.copy(latitude = start),
            date.month
        )
        val upper = HistoricTemperatureLookup.getMonthlyTemperatureRange(
            context,
            location.copy(latitude = end),
            date.month
        )

        val pct = SolMath.norm(location.latitude, start, end)

        val lerpedLow = SolMath.lerp(pct.toFloat(), lower.start.temperature, upper.start.temperature)
        val lerpedHigh = SolMath.lerp(pct.toFloat(), lower.end.temperature, upper.end.temperature)
        return Range(Temperature.celsius(lerpedLow), Temperature.celsius(lerpedHigh))
    }

}