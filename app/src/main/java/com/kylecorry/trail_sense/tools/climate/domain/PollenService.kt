package com.kylecorry.trail_sense.tools.climate.domain

import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.ecology.Ecology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.IWeatherSubsystem
import java.time.LocalDate

// Pollen is active while the cumulative GDD (since the winter low) is between the thresholds
enum class PollenType(
    val baseTemperature: Temperature,
    val startGddFahrenheit: Float,
    val endGddFahrenheit: Float
) {
    Tree(Temperature.fahrenheit(40f), 20f, 750f),
    Grass(Temperature.fahrenheit(50f), 200f, 1400f),
    Weed(Temperature.fahrenheit(60f), 780f, 1080f)
}

class PollenService(private val weather: IWeatherSubsystem) {

    private data class DailyGdd(val date: LocalDate, val cumulative: Float, val daily: Float)

    suspend fun getYearlyPollenPeriods(
        year: Int,
        location: Coordinate? = null,
        elevation: Distance? = null,
        calibrated: Boolean = true
    ): Map<PollenType, List<Range<LocalDate>>> = onDefault {
        val temperatures = weather.getTemperatureRanges(year, location, elevation, calibrated)
        val temperatureByDay = temperatures.associate { it.first.monthDay() to it.second }
        val winterLowDay = temperatures.minBy { it.second.start.celsius().value }.first.monthDay()

        PollenType.entries.associateWith { type ->
            val gdd = getDailyGdd(year, type.baseTemperature, winterLowDay, temperatureByDay)
            val start = fahrenheitToCelsiusDegrees(type.startGddFahrenheit)
            val end = fahrenheitToCelsiusDegrees(type.endGddFahrenheit)

            // Pollen is not released on days that are too cold for growth, even inside the GDD window
            getContiguousRanges(gdd) { it.cumulative in start..end && it.daily > 0f }
        }
    }

    // Starts a year early so the count at the start of the year is correct, and resets on the winter low
    private fun getDailyGdd(
        year: Int,
        baseTemperature: Temperature,
        winterLowDay: Pair<Int, Int>,
        temperatureByDay: Map<Pair<Int, Int>, Range<Temperature>>
    ): List<DailyGdd> {
        val result = mutableListOf<DailyGdd>()
        var cumulative = 0f
        var date = LocalDate.of(year - 1, 1, 1)

        while (date.year <= year) {
            if (date.monthDay() == winterLowDay) {
                cumulative = 0f
            }

            // Leap day uses Feb 28
            val temperature = temperatureByDay[date.monthDay()] ?: temperatureByDay.getValue(2 to 28)
            val daily = Ecology.getGrowingDegreeDays(temperature, baseTemperature, MAX_TEMPERATURE_C)
            cumulative += daily

            if (date.year == year) {
                result.add(DailyGdd(date, cumulative, daily))
            }
            date = date.plusDays(1)
        }
        return result
    }

    private fun getContiguousRanges(
        days: List<DailyGdd>,
        isActive: (DailyGdd) -> Boolean
    ): List<Range<LocalDate>> {
        val ranges = mutableListOf<Range<LocalDate>>()
        var start: LocalDate? = null
        var end: LocalDate? = null

        for (day in days) {
            if (isActive(day)) {
                start = start ?: day.date
                end = day.date
            } else if (start != null && end != null) {
                ranges.add(Range(start, end))
                start = null
            }
        }

        if (start != null && end != null) {
            ranges.add(Range(start, end))
        }
        return ranges
    }

    private fun LocalDate.monthDay(): Pair<Int, Int> = monthValue to dayOfMonth

    private fun fahrenheitToCelsiusDegrees(degrees: Float): Float = degrees * 5 / 9f

    companion object {
        private const val MAX_TEMPERATURE_C = 30f
    }
}
