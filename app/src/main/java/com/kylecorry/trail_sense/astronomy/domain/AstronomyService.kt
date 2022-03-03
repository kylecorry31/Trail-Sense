package com.kylecorry.trail_sense.astronomy.domain

import com.kylecorry.sol.science.astronomy.AstronomyService
import com.kylecorry.sol.science.astronomy.IAstronomyService
import com.kylecorry.sol.science.astronomy.RiseSetTransitTimes
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.science.astronomy.eclipse.EclipseType
import com.kylecorry.sol.science.astronomy.meteors.MeteorShowerPeak
import com.kylecorry.sol.science.astronomy.moon.MoonPhase
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.science.shared.Season
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.extensions.roundNearestMinute
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

    fun isSuperMoon(date: LocalDate): Boolean {
        val time = date.atTime(12, 0).toZonedDateTime()
        return newAstronomyService.isSuperMoon(time)
    }

    fun getMoonTimes(location: Coordinate, date: LocalDate): RiseSetTransitTimes {
        return newAstronomyService.getMoonEvents(date.atStartOfDay().toZonedDateTime(), location)
    }

    fun getCenteredMoonAltitudes(
        location: Coordinate,
        time: ZonedDateTime
    ): List<Reading<Float>> {
        val startTime = time.roundNearestMinute(10).minusHours(12)
        val granularityMinutes = 10L
        val altitudes = mutableListOf<Reading<Float>>()
        for (i in 0..Duration.ofDays(1).toMinutes() step granularityMinutes) {
            altitudes.add(
                Reading(
                    getMoonAltitude(location, startTime.plusMinutes(i)),
                    startTime.plusMinutes(i).toInstant()
                )
            )
        }
        return altitudes
    }

    fun getMoonAltitudes(location: Coordinate, date: LocalDate): List<Reading<Float>> {
        val start = date.atStartOfDay().toZonedDateTime()
        val totalTime = 24 * 60
        val granularityMinutes = 10
        val altitudes = mutableListOf<Reading<Float>>()
        for (i in 0..totalTime step granularityMinutes) {
            altitudes.add(
                Reading(
                    getMoonAltitude(location, start.plusMinutes(i.toLong())),
                    start.plusMinutes(i.toLong()).toInstant(),
                )
            )
        }
        return altitudes
    }

    fun getMoonAltitude(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now()): Float {
        return newAstronomyService.getMoonAltitude(time, location, true)
    }

    fun getMoonAzimuth(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now()): Bearing {
        return newAstronomyService.getMoonAzimuth(time, location)
    }

    fun isMoonUp(location: Coordinate): Boolean {
        return newAstronomyService.isMoonUp(ZonedDateTime.now(clock), location)
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
        return newAstronomyService.getDaylightLength(
            date.atStartOfDay().toZonedDateTime(),
            location,
            sunTimesMode
        )
    }

    fun getTodaySunTimes(location: Coordinate, sunTimesMode: SunTimesMode): RiseSetTransitTimes {
        return getSunTimes(location, sunTimesMode, LocalDate.now(clock))
    }

    fun getTomorrowSunTimes(location: Coordinate, sunTimesMode: SunTimesMode): RiseSetTransitTimes {
        return getSunTimes(location, sunTimesMode, LocalDate.now(clock).plusDays(1))
    }

    fun getSunAltitudes(location: Coordinate, date: LocalDate): List<Reading<Float>> {
        val start = date.atStartOfDay().toZonedDateTime()
        val totalTime = 24 * 60L
        val granularityMinutes = 10L
        val altitudes = mutableListOf<Reading<Float>>()
        for (i in 0..totalTime step granularityMinutes) {
            altitudes.add(
                Reading(
                    getSunAltitude(location, start.plusMinutes(i)),
                    start.plusMinutes(i).toInstant()
                )
            )
        }
        return altitudes
    }

    fun getCenteredSunAltitudes(
        location: Coordinate,
        time: ZonedDateTime
    ): List<Reading<Float>> {
        val startTime = time.roundNearestMinute(10).minusHours(12)
        val granularityMinutes = 10L
        val altitudes = mutableListOf<Reading<Float>>()
        for (i in 0..Duration.ofDays(1).toMinutes() step granularityMinutes) {
            altitudes.add(
                Reading(
                    getSunAltitude(location, startTime.plusMinutes(i)),
                    startTime.plusMinutes(i).toInstant()
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

    fun getSunAzimuth(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now()): Bearing {
        return newAstronomyService.getSunAzimuth(time, location)
    }

    fun getSunAltitude(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now()): Float {
        return newAstronomyService.getSunAltitude(time, location, true)
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

    fun getMeteorShowerPeakAltitude(peak: MeteorShowerPeak, location: Coordinate): Float {
        return newAstronomyService.getMeteorShowerAltitude(
            peak.shower,
            location,
            peak.peak.toInstant()
        )
    }

    fun getMeteorShowerPeakAzimuth(peak: MeteorShowerPeak, location: Coordinate): Bearing {
        return newAstronomyService.getMeteorShowerAzimuth(
            peak.shower,
            location,
            peak.peak.toInstant()
        )
    }

    fun getSeason(location: Coordinate, date: LocalDate = LocalDate.now()): Season {
        return newAstronomyService.getSeason(
            location,
            date.atStartOfDay(ZoneId.systemDefault())
        )
    }

    fun getLunarEclipse(
        location: Coordinate,
        date: LocalDate = LocalDate.now()
    ): LunarEclipse? {
        val nextEclipse = newAstronomyService.getNextEclipse(
            date.atStartOfDay(ZoneId.systemDefault()),
            location,
            EclipseType.PartialLunar
        ) ?: return null

        val start = nextEclipse.start.toZonedDateTime().toLocalDateTime()
        val end = nextEclipse.end.toZonedDateTime().toLocalDateTime()
        val peak = nextEclipse.maximum.toZonedDateTime().toLocalDateTime()

        if (start.toLocalDate() != date && end.toLocalDate() != date) {
            return null
        }

        return LunarEclipse(start, end, peak, nextEclipse.magnitude)
    }

    fun findNextEvent(
        event: AstronomyEvent,
        location: Coordinate,
        start: LocalDate = LocalDate.now(),
        maxSearch: Duration = Duration.ofDays(365 * 2L)
    ): LocalDate? {
        // TODO: Add method to get date of true moon phase in TS Core and remove the is in event logic
        var isInEvent = when (event) {
            AstronomyEvent.FullMoon -> getMoonPhase(start).phase == MoonTruePhase.Full
            AstronomyEvent.NewMoon -> getMoonPhase(start).phase == MoonTruePhase.New
            AstronomyEvent.QuarterMoon -> listOf(
                MoonTruePhase.FirstQuarter,
                MoonTruePhase.ThirdQuarter
            ).contains(getMoonPhase(start).phase)
            AstronomyEvent.MeteorShower -> getMeteorShower(
                location,
                start
            )?.peak?.toLocalDate() == start
            AstronomyEvent.LunarEclipse -> getLunarEclipse(
                location,
                start
            ) != null
            AstronomyEvent.Supermoon -> isSuperMoon(start)
        }
        var date = start.plusDays(1)
        val end = start.plusDays(maxSearch.toDays())
        while (date <= end) {
            val hasEvent = when (event) {
                AstronomyEvent.FullMoon -> getMoonPhase(date).phase == MoonTruePhase.Full
                AstronomyEvent.NewMoon -> getMoonPhase(date).phase == MoonTruePhase.New
                AstronomyEvent.QuarterMoon -> listOf(
                    MoonTruePhase.FirstQuarter,
                    MoonTruePhase.ThirdQuarter
                ).contains(getMoonPhase(date).phase)
                AstronomyEvent.MeteorShower -> getMeteorShower(
                    location,
                    date
                )?.peak?.toLocalDate() == date
                AstronomyEvent.LunarEclipse -> getLunarEclipse(
                    location,
                    date
                ) != null
                AstronomyEvent.Supermoon -> isSuperMoon(date)
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