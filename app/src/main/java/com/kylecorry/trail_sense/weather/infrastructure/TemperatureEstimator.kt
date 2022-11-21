package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Temperature
import java.time.Duration
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor

class TemperatureEstimator(private val context: Context) {

    fun getDailyTemperature(location: Coordinate, date: LocalDate): Temperature {
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
            val lerped = SolMath.lerp(pct.toFloat(), thisMonth.temperature, nextMonth.temperature)
            Temperature.celsius(lerped)
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
            val lerped = SolMath.lerp(pct.toFloat(), prevMonth.temperature, thisMonth.temperature)
            Temperature.celsius(lerped)
        }
    }

    private fun getMonthlyTemperature(location: Coordinate, date: LocalDate): Temperature {
        val start = floor(location.latitude)
        val end = ceil(location.latitude)

        if (start == end) {
            return HistoricTemperatureLookup.getMonthlyAverageTemperature(
                context,
                location,
                date.month
            )
        }

        val lower = HistoricTemperatureLookup.getMonthlyAverageTemperature(
            context,
            location.copy(latitude = start),
            date.month
        )
        val upper = HistoricTemperatureLookup.getMonthlyAverageTemperature(
            context,
            location.copy(latitude = end),
            date.month
        )

        val pct = SolMath.norm(location.latitude, start, end)

        val lerped = SolMath.lerp(pct.toFloat(), lower.temperature, upper.temperature)
        return Temperature.celsius(lerped)
    }

}