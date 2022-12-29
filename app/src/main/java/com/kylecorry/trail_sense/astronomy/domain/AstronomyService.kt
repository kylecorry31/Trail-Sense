package com.kylecorry.trail_sense.astronomy.domain

import com.kylecorry.sol.science.astronomy.Astronomy
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
import com.kylecorry.trail_sense.shared.extensions.getReadings
import java.time.*

/**
 * The facade for astronomy related services
 */
class AstronomyService(private val clock: Clock = Clock.systemDefaultZone()) {
    
    // PUBLIC MOON METHODS

    fun getCurrentMoonPhase(): MoonPhase {
        return Astronomy.getMoonPhase(ZonedDateTime.now(clock))
    }

    /**
     * Gets the moon phase at noon (should this be rise/set?)
     */
    fun getMoonPhase(date: LocalDate): MoonPhase {
        val time = date.atTime(12, 0).toZonedDateTime()
        return Astronomy.getMoonPhase(time)
    }

    fun isSuperMoon(date: LocalDate): Boolean {
        val time = date.atTime(12, 0).toZonedDateTime()
        return Astronomy.isSuperMoon(time)
    }

    fun getMoonTimes(location: Coordinate, date: LocalDate): RiseSetTransitTimes {
        return Astronomy.getMoonEvents(date.atStartOfDay().toZonedDateTime(), location, true)
    }

    fun getCenteredMoonAltitudes(
        location: Coordinate,
        time: ZonedDateTime
    ): List<Reading<Float>> {
        val startTime = time.minusHours(12)
        val endTime = time.plusHours(12)
        return getReadings(
            startTime,
            endTime,
            altitudeGranularity
        ) {
            getMoonAltitude(location, it)
        }
    }

    fun getMoonAltitudes(location: Coordinate, date: LocalDate): List<Reading<Float>> {
        return getReadings(
            date,
            ZoneId.systemDefault(),
            altitudeGranularity
        ) {
            getMoonAltitude(location, it)
        }
    }

    fun getMoonAltitude(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now()): Float {
        return Astronomy.getMoonAltitude(time, location, true)
    }

    fun getMoonAzimuth(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now()): Bearing {
        return Astronomy.getMoonAzimuth(time, location)
    }

    fun isMoonUp(location: Coordinate): Boolean {
        return Astronomy.isMoonUp(ZonedDateTime.now(clock), location, true)
    }

    // PUBLIC SUN METHODS

    fun getSunTimes(
        location: Coordinate,
        sunTimesMode: SunTimesMode,
        date: LocalDate
    ): RiseSetTransitTimes {
        return Astronomy.getSunEvents(
            date.atStartOfDay().toZonedDateTime(),
            location,
            sunTimesMode,
            true
        )
    }

    fun getLengthOfDay(
        location: Coordinate,
        sunTimesMode: SunTimesMode,
        date: LocalDate
    ): Duration {
        return Astronomy.getDaylightLength(
            date.atStartOfDay().toZonedDateTime(),
            location,
            sunTimesMode,
            true
        )
    }

    fun getTodaySunTimes(location: Coordinate, sunTimesMode: SunTimesMode): RiseSetTransitTimes {
        return getSunTimes(location, sunTimesMode, LocalDate.now(clock))
    }

    fun getTomorrowSunTimes(location: Coordinate, sunTimesMode: SunTimesMode): RiseSetTransitTimes {
        return getSunTimes(location, sunTimesMode, LocalDate.now(clock).plusDays(1))
    }

    fun getSunAltitudes(location: Coordinate, date: LocalDate): List<Reading<Float>> {
        return getReadings(
            date,
            ZoneId.systemDefault(),
            altitudeGranularity
        ) {
            getSunAltitude(location, it)
        }
    }

    fun getCenteredSunAltitudes(
        location: Coordinate,
        time: ZonedDateTime
    ): List<Reading<Float>> {
        val startTime = time.minusHours(12)
        val endTime = time.plusHours(12)
        return getReadings(
            startTime,
            endTime,
            altitudeGranularity
        ) {
            getSunAltitude(location, it)
        }
    }

    fun getNextSunset(location: Coordinate, sunTimesMode: SunTimesMode, time: ZonedDateTime = ZonedDateTime.now(clock)): LocalDateTime? {
        return Astronomy.getNextSunset(time, location, sunTimesMode, true)
            ?.toLocalDateTime()
    }

    fun getNextSunrise(location: Coordinate, sunTimesMode: SunTimesMode, time: ZonedDateTime = ZonedDateTime.now(clock)): LocalDateTime? {
        return Astronomy.getNextSunrise(time, location, sunTimesMode, true)
            ?.toLocalDateTime()
    }

    fun isSunUp(location: Coordinate): Boolean {
        return Astronomy.isSunUp(ZonedDateTime.now(clock), location, true)
    }

    fun getSunAzimuth(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now()): Bearing {
        return Astronomy.getSunAzimuth(time, location)
    }

    fun getSunAltitude(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now()): Float {
        return Astronomy.getSunAltitude(time, location, true)
    }

    fun getMeteorShower(
        location: Coordinate,
        date: LocalDate = LocalDate.now()
    ): MeteorShowerPeak? {
        val today = date.atTime(12, 0).toZonedDateTime()
        val todays = Astronomy.getMeteorShower(location, today)
        val tomorrows = Astronomy.getMeteorShower(location, today.plusDays(1))
        return todays ?: tomorrows
    }

    fun getMeteorShowerPeakAltitude(peak: MeteorShowerPeak, location: Coordinate): Float {
        return Astronomy.getMeteorShowerAltitude(
            peak.shower,
            location,
            peak.peak.toInstant()
        )
    }

    fun getMeteorShowerPeakAzimuth(peak: MeteorShowerPeak, location: Coordinate): Bearing {
        return Astronomy.getMeteorShowerAzimuth(
            peak.shower,
            location,
            peak.peak.toInstant()
        )
    }

    fun getSeason(location: Coordinate, date: LocalDate = LocalDate.now()): Season {
        return Astronomy.getSeason(
            location,
            date.atStartOfDay(ZoneId.systemDefault())
        )
    }

    fun getLunarEclipse(
        location: Coordinate,
        date: LocalDate = LocalDate.now()
    ): LunarEclipse? {
        val nextEclipse = Astronomy.getNextEclipse(
            date.atStartOfDay(ZoneId.systemDefault()),
            location,
            EclipseType.PartialLunar
        ) ?: return null

        val start = nextEclipse.start.toZonedDateTime()
        val end = nextEclipse.end.toZonedDateTime()
        val peak = nextEclipse.maximum.toZonedDateTime()

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

    companion object {
        private val altitudeGranularity = Duration.ofMinutes(10)
    }
}