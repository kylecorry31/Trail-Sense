package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.analysis.Trigonometry
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.time.Time.atStartOfDay
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.ceil
import kotlin.math.floor

internal class TemperatureEstimator(private val context: Context) {

    private val astronomy = AstronomyService()

    fun getTemperature(location: Coordinate, time: ZonedDateTime): Temperature {
        val range = getDailyTemperatureRange(location, time.toLocalDate())
        val today = astronomy.getSunTimes(location, SunTimesMode.Actual, time.toLocalDate())

        val defaultMaxTime = time.withHour(14).withMinute(0).withSecond(0).withNano(0)

        val todayMin = today.rise ?: time.atStartOfDay()
        val todayMax = today.transit?.plusHours(2) ?: defaultMaxTime

        return if (time.isAfter(todayMin) && time.isBefore(todayMax)) {
            // Sine between min and max
            val firstVec = Vector2(0f, range.start.temperature)
            val secondVec = Vector2(getX(todayMin, todayMax), range.end.temperature)
            val wave = Trigonometry.connect(firstVec, secondVec)
            val temperature = wave.calculate(getX(todayMin, time))
            Temperature.celsius(temperature)
        } else if (time.isBefore(todayMin)) {
            // Sine between yesterday max and min
            val yesterday = astronomy.getSunTimes(
                location,
                SunTimesMode.Actual,
                time.toLocalDate().minusDays(1)
            )
            val yesterdayMax = yesterday.transit?.plusHours(2) ?: defaultMaxTime.minusDays(1)

            val firstVec = Vector2(0f, range.end.temperature)
            val secondVec = Vector2(getX(yesterdayMax, todayMin), range.start.temperature)
            val wave = Trigonometry.connect(firstVec, secondVec)
            val temperature = wave.calculate(getX(yesterdayMax, time))
            Temperature.celsius(temperature)
        } else {
            // Sine between today max and tomorrow min
            val tomorrow =
                astronomy.getSunTimes(location, SunTimesMode.Actual, time.toLocalDate().plusDays(1))
            val tomorrowMin = tomorrow.rise ?: time.atStartOfDay().minusDays(1)

            val firstVec = Vector2(0f, range.end.temperature)
            val secondVec = Vector2(getX(todayMax, tomorrowMin), range.start.temperature)
            val wave = Trigonometry.connect(firstVec, secondVec)
            val temperature = wave.calculate(getX(todayMax, time))
            Temperature.celsius(temperature)
        }
    }

    private fun getX(start: ZonedDateTime, time: ZonedDateTime): Float {
        return Time.hoursBetween(start, time)
    }

    fun getDailyTemperatureRange(
        location: Coordinate,
        date: LocalDate
    ): Range<Temperature> {
        val average = getDailyTemperature(location, date)
        val range = getTemperatureRange(location)

        return Range(
            average.copy(temperature = average.temperature - range / 2f),
            average.copy(temperature = average.temperature + range / 2f)
        )
    }

    private fun getDailyTemperature(location: Coordinate, date: LocalDate): Temperature {
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

    private fun getTemperatureRange(location: Coordinate): Float {
        val start = floor(location.latitude)
        val end = ceil(location.latitude)

        if (start == end) {
            return HistoricTemperatureLookup.getTemperatureDiurnalRange(
                context,
                location
            )
        }

        val lower = HistoricTemperatureLookup.getTemperatureDiurnalRange(
            context,
            location.copy(latitude = start)
        )
        val upper = HistoricTemperatureLookup.getTemperatureDiurnalRange(
            context,
            location.copy(latitude = end)
        )

        val pct = SolMath.norm(location.latitude, start, end)

        return SolMath.lerp(pct.toFloat(), lower, upper)
    }

}