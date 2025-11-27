package com.kylecorry.trail_sense.tools.astronomy.domain

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.RiseSetTransitTimes
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.science.astronomy.eclipse.EclipseType
import com.kylecorry.sol.science.astronomy.locators.Planet
import com.kylecorry.sol.science.astronomy.meteors.MeteorShower
import com.kylecorry.sol.science.astronomy.meteors.MeteorShowerPeak
import com.kylecorry.sol.science.astronomy.moon.MoonPhase
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.science.astronomy.stars.CONSTELLATIONS
import com.kylecorry.sol.science.astronomy.stars.Constellation
import com.kylecorry.sol.science.astronomy.stars.STAR_CATALOG
import com.kylecorry.sol.science.astronomy.stars.Star
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.science.shared.Season
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

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
        return Astronomy.getMoonEvents(
            date.atStartOfDay().toZonedDateTime(),
            location,
            withRefraction = true,
            withParallax = true
        )
    }

    fun getCenteredMoonAltitudes(
        location: Coordinate,
        time: ZonedDateTime
    ): List<Reading<Float>> {
        val startTime = time.minusHours(12)
        val endTime = time.plusHours(12)
        return Time.getReadings(
            startTime,
            endTime,
            altitudeGranularity
        ) {
            getMoonAltitude(location, it)
        }
    }

    fun getMoonAltitudes(location: Coordinate, date: LocalDate): List<Reading<Float>> {
        return Time.getReadings(
            date,
            ZoneId.systemDefault(),
            altitudeGranularity
        ) {
            getMoonAltitude(location, it)
        }
    }

    fun getMoonAltitude(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now()): Float {
        return Astronomy.getMoonAltitude(time, location, withRefraction = true, withParallax = true)
    }

    fun getMoonAzimuth(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now()): Bearing {
        return Astronomy.getMoonAzimuth(time, location, withParallax = true)
    }

    fun isMoonUp(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now(clock)): Boolean {
        return Astronomy.isMoonUp(
            time,
            location,
            withRefraction = true,
            withParallax = true
        )
    }

    fun getMoonAboveHorizonTimes(location: Coordinate, time: ZonedDateTime): Range<ZonedDateTime>? {
        return Astronomy.getMoonAboveHorizonTimes(
            location,
            time,
            withRefraction = true,
            withParallax = true
        )
    }

    fun getMoonTilt(
        location: Coordinate,
        time: ZonedDateTime = ZonedDateTime.now(),
        useNearestTransit: Boolean = false
    ): Float {
        val timeToUse = if (useNearestTransit) {
            getMoonTimes(location, time.toLocalDate()).transit ?: getMoonTimes(
                location,
                time.toLocalDate().minusDays(1)
            ).transit ?: time.toLocalDate().atStartOfDay().toZonedDateTime()
        } else {
            time
        }


        return Astronomy.getMoonParallacticAngle(timeToUse, location)
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
        return Time.getReadings(
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
        return Time.getReadings(
            startTime,
            endTime,
            altitudeGranularity
        ) {
            getSunAltitude(location, it)
        }
    }

    fun getNextSunset(
        location: Coordinate,
        sunTimesMode: SunTimesMode,
        time: ZonedDateTime = ZonedDateTime.now(clock)
    ): LocalDateTime? {
        return Astronomy.getNextSunset(time, location, sunTimesMode, true)
            ?.toLocalDateTime()
    }

    fun getNextSunrise(
        location: Coordinate,
        sunTimesMode: SunTimesMode,
        time: ZonedDateTime = ZonedDateTime.now(clock)
    ): LocalDateTime? {
        return Astronomy.getNextSunrise(time, location, sunTimesMode, true)
            ?.toLocalDateTime()
    }

    fun isSunUp(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now(clock)): Boolean {
        return Astronomy.isSunUp(time, location, true)
    }

    fun getSunAzimuth(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now()): Bearing {
        return Astronomy.getSunAzimuth(time, location)
    }

    fun getSunAltitude(location: Coordinate, time: ZonedDateTime = ZonedDateTime.now()): Float {
        return Astronomy.getSunAltitude(time, location, true)
    }

    fun getSunAboveHorizonTimes(location: Coordinate, time: ZonedDateTime): Range<ZonedDateTime>? {
        return Astronomy.getSunAboveHorizonTimes(
            location, time,
            withRefraction = true
        )
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
    ): Eclipse? {
        return getEclipse(location, date, EclipseType.PartialLunar) {
            getMoonAzimuth(location, it) to getMoonAltitude(location, it)
        }
    }

    fun getSolarEclipse(
        location: Coordinate,
        date: LocalDate = LocalDate.now()
    ): Eclipse? {
        return getEclipse(location, date, EclipseType.Solar, 1) {
            getSunAzimuth(location, it) to getSunAltitude(location, it)
        }
    }

    fun getVisibleStars(
        location: Coordinate,
        time: ZonedDateTime = ZonedDateTime.now(),
        thresholdElevation: Float? = 0f
    ): List<Pair<Star, Pair<Bearing, Float>>> {
        if (isSunUp(location, time)) {
            return emptyList()
        }

        return STAR_CATALOG.filter { it.magnitude <= 4.0 }.map {
            val azimuth = Astronomy.getStarAzimuth(it, time, location)
            val altitude = Astronomy.getStarAltitude(it, time, location, true)
            it to (azimuth to altitude)
        }.filter { thresholdElevation == null || it.second.second > thresholdElevation }
    }

    fun getConstellationsForStar(star: Star): List<Constellation> {
        return CONSTELLATIONS.filter { it.allStarIds.contains(star.hipDesignation) }
    }

    fun getStarPosition(
        star: Star,
        location: Coordinate,
        time: ZonedDateTime = ZonedDateTime.now()
    ): CelestialObservation {
        val azimuth = Astronomy.getStarAzimuth(star, time, location)
        val altitude = Astronomy.getStarAltitude(star, time, location, true)
        return CelestialObservation(azimuth, altitude)
    }

    fun getVisiblePlanets(
        location: Coordinate,
        time: ZonedDateTime = ZonedDateTime.now(),
        thresholdElevation: Float? = 0f
    ): List<Pair<Planet, CelestialObservation>> {
        if (isSunUp(location, time)) {
            return emptyList()
        }

        // Initial filter based on magnitude / proximity to the sun
        val planetsToConsider = listOf(
            Planet.Venus,
            Planet.Mars,
            Planet.Jupiter,
            Planet.Saturn
        )

        return planetsToConsider.map {
            it to Astronomy.getPlanetPosition(it, time, location, withRefraction = true)
        }.filter { thresholdElevation == null || it.second.altitude > thresholdElevation }
    }

    fun getVisibleMeteorShowers(
        location: Coordinate,
        time: ZonedDateTime = ZonedDateTime.now(),
        thresholdElevation: Float? = -10f
    ): List<Pair<MeteorShower, CelestialObservation>> {
        if (isSunUp(location, time)) {
            return emptyList()
        }

        val showers = Astronomy.getActiveMeteorShowers(location, time)

        return showers
            .filter {
                Duration.between(time, it.peak)
                    .abs() <= Duration.ofDays(it.shower.activeDays.toLong() / 3)
            }
            .map { shower ->
                shower.shower to Astronomy.getMeteorShowerPosition(
                    shower.shower,
                    location,
                    time.toInstant()
                )
            }.filter { thresholdElevation == null || it.second.altitude > thresholdElevation }
    }

    private fun getEclipse(
        location: Coordinate,
        date: LocalDate,
        eclipseType: EclipseType,
        days: Long = 2,
        getPosition: (ZonedDateTime) -> Pair<Bearing, Float>
    ): Eclipse? {
        val nextEclipse = Astronomy.getNextEclipse(
            date.atStartOfDay(ZoneId.systemDefault()),
            location,
            eclipseType,
            Duration.ofDays(days)
        ) ?: return null

        val start = nextEclipse.start.toZonedDateTime()
        val end = nextEclipse.end.toZonedDateTime()
        val peak = nextEclipse.maximum.toZonedDateTime()

        if (start.toLocalDate() != date && end.toLocalDate() != date) {
            return null
        }

        val position = getPosition(peak)

        return Eclipse(
            start,
            end,
            peak,
            nextEclipse.magnitude,
            nextEclipse.obscuration,
            position.second,
            position.first
        )
    }

    fun findNextEvent(
        event: AstronomyEvent,
        location: Coordinate,
        start: LocalDate = LocalDate.now(),
        maxSearch: Duration = Duration.ofDays(365 * 2L)
    ): LocalDate? {
        // An update to this algorithm is planned to make it more efficient
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
            AstronomyEvent.SolarEclipse -> getSolarEclipse(
                location,
                start
            ) != null
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
                AstronomyEvent.SolarEclipse -> getSolarEclipse(
                    location,
                    date
                ) != null
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
        const val SUN_MIN_ALTITUDE_CIVIL = -6f
        const val SUN_MIN_ALTITUDE_NAUTICAL = -12f
        const val SUN_MIN_ALTITUDE_ASTRONOMICAL = -18f
    }
}