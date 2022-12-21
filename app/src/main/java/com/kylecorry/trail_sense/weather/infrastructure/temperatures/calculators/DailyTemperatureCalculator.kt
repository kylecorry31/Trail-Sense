package com.kylecorry.trail_sense.weather.infrastructure.temperatures.calculators

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.time.Time.getClosestFutureTime
import com.kylecorry.sol.time.Time.getClosestPastTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

internal class DailyTemperatureCalculator(
    private val location: Coordinate,
    private val dailyTemperatureRangeProvider: suspend (location: Coordinate, date: LocalDate) -> Range<Temperature>
) : ITemperatureCalculator {

    private val calculators = mutableListOf<Pair<Range<ZonedDateTime>, ITemperatureCalculator>>()
    private val offset = Duration.ofHours(3)

    override suspend fun calculate(time: ZonedDateTime): Temperature {
        val existing = calculators.firstOrNull { it.first.contains(time) }
        val calculator = if (existing == null) {
            val newCalculator = createCalculator(time)
            calculators.add(newCalculator)
            newCalculator.second
        } else {
            existing.second
        }
        return calculator.calculate(time)
    }

    private suspend fun createCalculator(time: ZonedDateTime): Pair<Range<ZonedDateTime>, ITemperatureCalculator> {
        val range = dailyTemperatureRangeProvider.invoke(location, time.toLocalDate())
        val nextHigh = getNextHighTime(time)
        val nextLow = getNextLowTime(time)
        val previousHigh = getPreviousHighTime(time)
        val previousLow = getPreviousLowTime(time)

        // Low High x Low High
        if (nextLow.isBefore(nextHigh)) {

            val start = if (nextLow.toLocalDate() != time.toLocalDate()) {
                dailyTemperatureRangeProvider.invoke(location, nextLow.toLocalDate()).start
            } else {
                range.start
            }

            val end = if (previousHigh.toLocalDate() != time.toLocalDate()) {
                dailyTemperatureRangeProvider.invoke(location, previousHigh.toLocalDate()).end
            } else {
                range.end
            }

            return Range(previousHigh, nextLow) to QuadraticTemperatureCalculator(
                Reading(start, nextLow.toInstant()),
                Reading(end, previousHigh.toInstant())
            )
        }

        val start = if (previousLow.toLocalDate() != time.toLocalDate()) {
            dailyTemperatureRangeProvider.invoke(location, previousLow.toLocalDate()).start
        } else {
            range.start
        }

        val end = if (nextHigh.toLocalDate() != time.toLocalDate()) {
            dailyTemperatureRangeProvider.invoke(location, nextHigh.toLocalDate()).end
        } else {
            range.end
        }

        // High Low x High Low
        return Range(previousLow, nextHigh) to SineTemperatureCalculator(
            Reading(start, previousLow.toInstant()),
            Reading(end, nextHigh.toInstant())
        )
    }


    private fun getNextHighTime(time: ZonedDateTime): ZonedDateTime {
        val today = Astronomy.getSunEvents(time, location, SunTimesMode.Actual, false)
        val tomorrow =
            Astronomy.getSunEvents(time.plusDays(1), location, SunTimesMode.Actual, false)
        return getClosestFutureTime(
            time,
            listOf(today.transit?.plus(offset), tomorrow.transit?.plus(offset))
        ) ?: getClosestFutureTime(
            time,
            listOf(
                ZonedDateTime.of(time.toLocalDate(), LocalTime.NOON.plus(offset), time.zone),
                ZonedDateTime.of(
                    time.toLocalDate().plusDays(1),
                    LocalTime.NOON.plus(offset),
                    time.zone
                )
            )
        )!!
    }

    private fun getNextLowTime(time: ZonedDateTime): ZonedDateTime {
        return Astronomy.getNextSunrise(time, location) ?: getClosestFutureTime(
            time,
            listOf(
                ZonedDateTime.of(time.toLocalDate(), LocalTime.MIN, time.zone),
                ZonedDateTime.of(time.toLocalDate().plusDays(1), LocalTime.MIN, time.zone)
            )
        )!!
    }

    private fun getPreviousHighTime(time: ZonedDateTime): ZonedDateTime {
        val today = Astronomy.getSunEvents(time, location, SunTimesMode.Actual, false)
        val yesterday =
            Astronomy.getSunEvents(time.minusDays(1), location, SunTimesMode.Actual, false)
        return getClosestPastTime(
            time,
            listOf(today.transit?.plus(offset), yesterday.transit?.plus(offset))
        ) ?: getClosestPastTime(
            time,
            listOf(
                ZonedDateTime.of(time.toLocalDate(), LocalTime.NOON.plus(offset), time.zone),
                ZonedDateTime.of(
                    time.toLocalDate().minusDays(1),
                    LocalTime.NOON.plus(offset),
                    time.zone
                )
            )
        )!!
    }

    private fun getPreviousLowTime(time: ZonedDateTime): ZonedDateTime {
        val today = Astronomy.getSunEvents(time, location, SunTimesMode.Actual, false)
        val yesterday =
            Astronomy.getSunEvents(time.minusDays(1), location, SunTimesMode.Actual, false)
        return getClosestPastTime(
            time,
            listOf(today.rise, yesterday.rise)
        ) ?: getClosestPastTime(
            time,
            listOf(
                ZonedDateTime.of(time.toLocalDate(), LocalTime.MIN, time.zone),
                ZonedDateTime.of(
                    time.toLocalDate().minusDays(1),
                    LocalTime.MIN,
                    time.zone
                )
            )
        )!!
    }
}