package com.kylecorry.trail_sense.astronomy.domain

import com.kylecorry.trail_sense.astronomy.domain.moon.*
import com.kylecorry.trail_sense.astronomy.domain.sun.*
import com.kylecorry.trail_sense.astronomy.infrastructure.AstronomyPreferences
import com.kylecorry.trail_sense.shared.Coordinate
import org.threeten.bp.*
import com.kylecorry.trail_sense.astronomy.domain.sun.SunTimesMode as SunTimesMode

class AstronomyService(private val clock: Clock = Clock.systemDefaultZone()) {

    private val moonPhaseCalculator = MoonPhaseCalculator()
    private val moonTimesCalculator = AltitudeMoonTimesCalculator()
    private val moonStateCalculator = MoonStateCalculator()

    // PUBLIC MOON PROPERTIES

    fun getCurrentMoonPhase(): MoonPhase {
        return moonPhaseCalculator.getPhase(ZonedDateTime.now(clock))
    }

    fun getMoonTimes(location: Coordinate, date: LocalDate): MoonTimes {
        return moonTimesCalculator.calculate(location, date)
    }

    fun isMoonUp(location: Coordinate): Boolean {
        return moonStateCalculator.isUp(getTodayMoonTimes(location), LocalTime.now(clock))
    }

    fun getLastMoonSet(location: Coordinate): LocalDateTime? {
        val today = getTodayMoonTimes(location)
        val yesterday = getYesterdayMoonTimes(location)
        return DateUtils.getClosestPastTime(LocalDateTime.now(clock), listOf(today.down, yesterday.down))
    }

    fun getLastMoonRise(location: Coordinate): LocalDateTime? {
        val today = getTodayMoonTimes(location)
        val yesterday = getYesterdayMoonTimes(location)
        return DateUtils.getClosestPastTime(LocalDateTime.now(clock), listOf(today.up, yesterday.up))
    }

    fun getNextMoonSet(location: Coordinate): LocalDateTime? {
        val today = getTodayMoonTimes(location)
        val tomorrow = getTomorrowMoonTimes(location)
        return DateUtils.getClosestFutureTime(LocalDateTime.now(clock), listOf(today.down, tomorrow.down))
    }

    fun getNextMoonRise(location: Coordinate): LocalDateTime? {
        val today = getTodayMoonTimes(location)
        val tomorrow = getTomorrowMoonTimes(location)
        return DateUtils.getClosestFutureTime(LocalDateTime.now(clock), listOf(today.up, tomorrow.up))
    }

    // PUBLIC SUN PROPERTIES

    fun getSunTimes(location: Coordinate, sunTimesMode: SunTimesMode, date: LocalDate): SunTimes {
        return when (sunTimesMode){
            SunTimesMode.Actual -> ActualTwilightCalculator().calculate(location, date)
            SunTimesMode.Civil -> CivilTwilightCalculator().calculate(location, date)
            SunTimesMode.Nautical -> NauticalTwilightCalculator().calculate(location, date)
            SunTimesMode.Astronomical -> AstronomicalTwilightCalculator().calculate(location, date)
        }
    }

    fun getTodaySunTimes(location: Coordinate, sunTimesMode: SunTimesMode): SunTimes {
        return getSunTimes(location, sunTimesMode, LocalDate.now(clock))
    }

    fun getTomorrowSunTimes(location: Coordinate, sunTimesMode: SunTimesMode): SunTimes {
        return getSunTimes(location, sunTimesMode, LocalDate.now(clock).plusDays(1))
    }

    fun getYesterdaySunTimes(location: Coordinate, sunTimesMode: SunTimesMode): SunTimes {
        return getSunTimes(location, sunTimesMode, LocalDate.now(clock).minusDays(1))
    }


    // PRIVATE MOON PROPERTIES

    private fun getTodayMoonTimes(location: Coordinate): MoonTimes {
        return getMoonTimes(location, LocalDate.now(clock))
    }

    private fun getYesterdayMoonTimes(location: Coordinate): MoonTimes {
        return getMoonTimes(location, LocalDate.now(clock).minusDays(1))
    }

    private fun getTomorrowMoonTimes(location: Coordinate): MoonTimes {
        return getMoonTimes(location, LocalDate.now(clock).plusDays(1))
    }

}