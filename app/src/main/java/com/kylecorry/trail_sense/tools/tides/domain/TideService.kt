package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

class TideService {

    private val astronomyService = AstronomyService()

    fun getTidalRange(date: LocalDate): com.kylecorry.trailsensecore.domain.astronomy.tides.Tide {
        return astronomyService.getTides(date)
    }

    fun getSimpleTideType(referenceHighTide: ZonedDateTime, now: ZonedDateTime = ZonedDateTime.now()): TideType {
        val nextTide = getNextSimpleTide(referenceHighTide, now) ?: return TideType.Half
        val timeToNextTide = Duration.between(now, nextTide.time)
        return if (nextTide.isHigh && timeToNextTide < Duration.ofHours(2) || (!nextTide.isHigh && timeToNextTide > Duration.ofHours(4))){
            TideType.High
        } else if (!nextTide.isHigh && timeToNextTide < Duration.ofHours(2) || (nextTide.isHigh && timeToNextTide > Duration.ofHours(4))) {
            TideType.Low
        } else {
            TideType.Half
        }
    }

    fun getNextSimpleTide(referenceHighTide: ZonedDateTime, now: ZonedDateTime = ZonedDateTime.now()): Tide? {
        val today = getSimpleTides(referenceHighTide, now.toLocalDate())
        val tomorrow = getSimpleTides(referenceHighTide, now.toLocalDate().plusDays(1))

        return (today + tomorrow).firstOrNull {
            it.time > now
        }
    }

    fun getSimpleTides(referenceHighTide: ZonedDateTime, date: LocalDate = LocalDate.now()): List<Tide> {
        val averageLunarDay = Duration.ofHours(24).plusMinutes(50).plusSeconds(30)
        val halfLunarDay = averageLunarDay.dividedBy(2)
        val quarterLunarDay = averageLunarDay.dividedBy(4)
        var highTideOnDate = referenceHighTide
        while (highTideOnDate.toLocalDate() != date) {
            highTideOnDate = if (highTideOnDate.toLocalDate() > date) {
                highTideOnDate.minus(getLunarDayLength(highTideOnDate.toLocalDate()))
            } else {
                highTideOnDate.plus(getLunarDayLength(highTideOnDate.toLocalDate()))
            }
        }

        val tides = listOf(
            Tide(highTideOnDate.minus(halfLunarDay), true),
            Tide(highTideOnDate, true),
            Tide(highTideOnDate.plus(halfLunarDay), true),
            Tide(highTideOnDate.minus(halfLunarDay).minus(quarterLunarDay), false),
            Tide(highTideOnDate.minus(quarterLunarDay), false),
            Tide(highTideOnDate.plus(quarterLunarDay), false),
            Tide(highTideOnDate.plus(halfLunarDay).plus(quarterLunarDay), false),
        )

        return tides.filter { it.time.toLocalDate() == date }.sortedBy { it.time }


    }

    fun getLunarDayLength(date: LocalDate): Duration {
        val today = astronomyService.getMoonTimes(Coordinate.zero, date)
        val tomorrow = astronomyService.getMoonTimes(Coordinate.zero, date.plusDays(1))
        if (today.transit == null || tomorrow.transit == null){
            return getLunarDayLength(date.minusDays(1))
        }
        return Duration.between(today.transit, tomorrow.transit)
    }

    fun getTides(
        lastHighTide: ZonedDateTime,
        lastLowTide: ZonedDateTime?,
        date: LocalDate
    ): List<Tide> {
        // TODO: Handle if last high tide is before this
        var highTideOnDate = lastHighTide
        var lowTideOnDate = lastLowTide ?: lastHighTide.minus(getLunarDayLength(lastHighTide.toLocalDate()).dividedBy(4))

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