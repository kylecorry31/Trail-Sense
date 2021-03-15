package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

class TideService {

    private val astronomyService = AstronomyService()

    fun getLunarDayLength(date: LocalDate): Duration {
//        return Duration.ofHours(24).plusMinutes(50)
        val today = astronomyService.getMoonTimes(Coordinate.zero, date)
        val tomorrow = astronomyService.getMoonTimes(Coordinate.zero, date.plusDays(1))
        return Duration.between(today.transit, tomorrow.transit)
    }

    fun getTides(
        lastHighTide: ZonedDateTime,
        lastLowTide: ZonedDateTime?,
        date: LocalDate
    ): List<Tide> {
        // TODO: Handle if last high tide is before this
        var highTideOnDate = lastHighTide
        var lowTideOnDate = lastLowTide ?: lastHighTide.minus(Duration.ofHours(7).plusMinutes(12).plusSeconds(30))

        while (highTideOnDate.toLocalDate() != date) {
            highTideOnDate = if (highTideOnDate.toLocalDate() > date) {
                highTideOnDate.minus(getLunarDayLength(highTideOnDate.toLocalDate()))
            } else {
                highTideOnDate.plus(getLunarDayLength(highTideOnDate.toLocalDate()))
            }
        }

        while (lowTideOnDate.toLocalDate() != date) {
            lowTideOnDate = if (lowTideOnDate.toLocalDate() > date) {
                lowTideOnDate.minus(getLunarDayLength(lowTideOnDate.toLocalDate()))
            } else {
                lowTideOnDate.plus(getLunarDayLength(lowTideOnDate.toLocalDate()))
            }
        }

        val lunarDay = getLunarDayLength(date)
        val halfLunarDay = lunarDay.dividedBy(2)


        val previousHigh = highTideOnDate.minus(halfLunarDay)
        val nextHigh = highTideOnDate.plus(halfLunarDay)

        val previousLow = lowTideOnDate.minus(halfLunarDay)
        val nextLow = lowTideOnDate.plus(halfLunarDay)


        val tides = listOf(
            Tide(previousHigh, true),
            Tide(highTideOnDate, true),
            Tide(nextHigh, true),
            Tide(previousLow, false),
            Tide(lowTideOnDate, false),
            Tide(nextLow, false),
        )

        return tides.filter { it.time.toLocalDate() == date }.sortedBy { it.time }
    }

}