package com.kylecorry.trail_sense.astronomy.domain

import com.kylecorry.trailsensecore.domain.geo.Bearing
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trail_sense.shared.roundNearestMinute
import com.kylecorry.trail_sense.shared.toZonedDateTime
import com.kylecorry.trailsensecore.domain.astronomy.*
import com.kylecorry.trailsensecore.domain.astronomy.AstronomyService
import com.kylecorry.trailsensecore.domain.astronomy.moon.MoonPhase
import com.kylecorry.trailsensecore.domain.astronomy.moon.MoonTruePhase
import com.kylecorry.trailsensecore.domain.time.Season
import com.kylecorry.trailsensecore.domain.time.atStartOfDay
import java.time.*

/**
 * The facade for astronomy related services
 */
class AstronomyService(private val clock: Clock = Clock.systemDefaultZone()) {

    private val newAstronomyService: IAstronomyService = AstronomyService()

    // PUBLIC MOON METHODS

    fun getCurrentMoonPhase(): MoonPhase {
        return newAstronomyService.getMoonPhase(ZonedDateTime.now(clock))
    }

    /**
     * Gets the moon phase at noon (should this be rise/set?)
     */
    fun getMoonPhase(date: LocalDate): MoonPhase {
        val time = date.atTime(12, 0).toZonedDateTime()
        return newAstronomyService.getMoonPhase(time)
    }

    fun getMoonTimes(location: Coordinate, date: LocalDate): RiseSetTransitTimes {
        return newAstronomyService.getMoonEvents(date.atStartOfDay().toZonedDateTime(), location)
    }

    fun getCenteredMoonAltitudes(
        location: Coordinate,
        time: LocalDateTime
    ): List<Pair<LocalDateTime, Float>> {
        val startTime = time.roundNearestMinute(10).minusHours(12)
        val granularityMinutes = 10L
        val altitudes = mutableListOf<Pair<LocalDateTime, Float>>()
        for (i in 0..Duration.ofDays(1).toMinutes() step granularityMinutes) {
            altitudes.add(
                Pair(
                    startTime.plusMinutes(i),
                    getMoonAltitude(location, startTime.plusMinutes(i))
                )
            )
        }
        return altitudes
    }

    fun getMoonAltitudes(location: Coordinate, date: LocalDate): List<Pair<LocalDateTime, Float>> {
        val totalTime = 24 * 60
        val granularityMinutes = 10
        val altitudes = mutableListOf<Pair<LocalDateTime, Float>>()
        for (i in 0..totalTime step granularityMinutes) {
            altitudes.add(
                Pair(
                    date.atStartOfDay().plusMinutes(i.toLong()),
                    getMoonAltitude(location, date.atStartOfDay().plusMinutes(i.toLong()))
                )
            )
        }
        return altitudes
    }

    fun getMoonAltitude(location: Coordinate, time: LocalDateTime = LocalDateTime.now()): Float {
        return newAstronomyService.getMoonAltitude(time.toZonedDateTime(), location, true)
    }

    fun getMoonAzimuth(location: Coordinate): Bearing {
        return newAstronomyService.getMoonAzimuth(ZonedDateTime.now(clock), location)
    }

    fun isMoonUp(location: Coordinate): Boolean {
        return newAstronomyService.isMoonUp(ZonedDateTime.now(clock), location)
    }

    fun getLunarNoon(location: Coordinate, date: LocalDate = LocalDate.now()): LocalDateTime? {
        return getMoonTimes(location, date).transit?.toLocalDateTime()
    }

    // PUBLIC SUN METHODS

    fun getSunTimes(
        location: Coordinate,
        sunTimesMode: SunTimesMode,
        date: LocalDate
    ): RiseSetTransitTimes {
        return newAstronomyService.getSunEvents(
            date.atStartOfDay().toZonedDateTime(),
            location,
            sunTimesMode
        )
    }

    fun getLengthOfDay(
        location: Coordinate,
        sunTimesMode: SunTimesMode,
        date: LocalDate
    ): Duration {
        return newAstronomyService.getDaylightLength(date.atStartOfDay().toZonedDateTime(), location, sunTimesMode)
    }

    fun getTodaySunTimes(location: Coordinate, sunTimesMode: SunTimesMode): RiseSetTransitTimes {
        return getSunTimes(location, sunTimesMode, LocalDate.now(clock))
    }

    fun getTomorrowSunTimes(location: Coordinate, sunTimesMode: SunTimesMode): RiseSetTransitTimes {
        return getSunTimes(location, sunTimesMode, LocalDate.now(clock).plusDays(1))
    }

    fun getSunAltitudes(location: Coordinate, date: LocalDate): List<Pair<LocalDateTime, Float>> {
        val totalTime = 24 * 60L
        val granularityMinutes = 10L
        val altitudes = mutableListOf<Pair<LocalDateTime, Float>>()
        for (i in 0..totalTime step granularityMinutes) {
            altitudes.add(
                Pair(
                    date.atStartOfDay().plusMinutes(i),
                    getSunAltitude(location, date.atStartOfDay().plusMinutes(i))
                )
            )
        }
        return altitudes
    }

    fun getCenteredSunAltitudes(
        location: Coordinate,
        time: LocalDateTime
    ): List<Pair<LocalDateTime, Float>> {
        val startTime = time.roundNearestMinute(10).minusHours(12)
        val granularityMinutes = 10L
        val altitudes = mutableListOf<Pair<LocalDateTime, Float>>()
        for (i in 0..Duration.ofDays(1).toMinutes() step granularityMinutes) {
            altitudes.add(
                Pair(
                    startTime.plusMinutes(i),
                    getSunAltitude(location, startTime.plusMinutes(i))
                )
            )
        }
        return altitudes
    }

    fun getNextSunset(location: Coordinate, sunTimesMode: SunTimesMode): LocalDateTime? {
        return newAstronomyService.getNextSunset(ZonedDateTime.now(clock), location, sunTimesMode)
            ?.toLocalDateTime()
    }

    fun getNextSunrise(location: Coordinate, sunTimesMode: SunTimesMode): LocalDateTime? {
        return newAstronomyService.getNextSunrise(ZonedDateTime.now(clock), location, sunTimesMode)
            ?.toLocalDateTime()
    }

    fun isSunUp(location: Coordinate): Boolean {
        return newAstronomyService.isSunUp(ZonedDateTime.now(clock), location)
    }

    fun getSunAzimuth(location: Coordinate): Bearing {
        return newAstronomyService.getSunAzimuth(ZonedDateTime.now(clock), location)
    }

    fun getSolarNoon(location: Coordinate, date: LocalDate = LocalDate.now()): LocalDateTime? {
        return getSunTimes(location, SunTimesMode.Actual, date).transit?.toLocalDateTime()
    }

    fun getSunAltitude(location: Coordinate, time: LocalDateTime = LocalDateTime.now()): Float {
        return newAstronomyService.getSunAltitude(time.toZonedDateTime(), location, true)
    }

    fun getMeteorShower(
        location: Coordinate,
        date: LocalDate = LocalDate.now()
    ): MeteorShowerPeak? {
        val today = date.atTime(12, 0).toZonedDateTime()
        val todays = newAstronomyService.getMeteorShower(location, today)
        val tomorrows = newAstronomyService.getMeteorShower(location, today.plusDays(1))
        return todays ?: tomorrows
    }

    fun getSeason(location: Coordinate, date: LocalDate = LocalDate.now()): Season {
        return newAstronomyService.getAstronomicalSeason(
            location,
            date.atStartOfDay(ZoneId.systemDefault())
        )
    }

    fun findNextEvent(
        event: AstronomyEvent,
        location: Coordinate,
        start: LocalDate = LocalDate.now(),
        maxSearch: Duration = Duration.ofDays(365)
    ): LocalDate? {
        // TODO: Add method to get date of true moon phase in TS Core and remove the is in event logic
        var isInEvent = when (event) {
            AstronomyEvent.FullMoon -> getMoonPhase(start).phase == MoonTruePhase.Full
            AstronomyEvent.NewMoon -> getMoonPhase(start).phase == MoonTruePhase.New
            AstronomyEvent.MeteorShower -> getMeteorShower(
                location,
                start
            )?.peak?.toLocalDate() == start
        }
        var date = start.plusDays(1)
        val end = start.plusDays(maxSearch.toDays())
        while (date <= end) {
            val hasEvent = when (event) {
                AstronomyEvent.FullMoon -> getMoonPhase(date).phase == MoonTruePhase.Full
                AstronomyEvent.NewMoon -> getMoonPhase(date).phase == MoonTruePhase.New
                AstronomyEvent.MeteorShower -> getMeteorShower(
                    location,
                    date
                )?.peak?.toLocalDate() == date
            }
            if (hasEvent && !isInEvent) {
                return date
            }
            isInEvent = hasEvent
            date = date.plusDays(1)
        }
        return null
    }
}