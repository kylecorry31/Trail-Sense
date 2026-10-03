package com.kylecorry.trail_sense.tools.astronomy.domain

import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.science.shared.Season
import com.kylecorry.sol.units.Coordinate
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.TimeZone

/**
 * Checks the astronomy facade against well known published events, so a regression in how the
 * app calls the underlying astronomy library (units, time zones, search logic) is caught.
 */
internal class AstronomyServiceTest {

    private val zone = ZoneId.of("America/New_York")
    private lateinit var originalTimeZone: TimeZone
    private val service = AstronomyService()

    // Near Boston, MA (sunrise and sunset published by timeanddate.com were used as the reference)
    private val boston = Coordinate(42.36, -71.06)
    private val cleveland = Coordinate(41.50, -81.69)

    @BeforeEach
    fun setup() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
    }

    @AfterEach
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    // SUN

    @Test
    fun sunTimesOnTheSummerSolstice() {
        val times = service.getSunTimes(boston, SunTimesMode.Actual, LocalDate.of(2025, 6, 21))

        assertTimeNear(LocalTime.of(5, 7), times.rise)
        // Solar noon is late because of daylight saving time
        assertTimeNear(LocalTime.of(12, 46), times.transit)
        assertTimeNear(LocalTime.of(20, 25), times.set)
    }

    @Test
    fun sunTimesOnTheWinterSolstice() {
        val times = service.getSunTimes(boston, SunTimesMode.Actual, LocalDate.of(2025, 12, 21))

        assertTimeNear(LocalTime.of(7, 11), times.rise)
        assertTimeNear(LocalTime.of(11, 42), times.transit)
        assertTimeNear(LocalTime.of(16, 14), times.set)
    }

    @Test
    fun civilTwilightStartsBeforeSunrise() {
        val date = LocalDate.of(2025, 6, 21)

        val actual = service.getSunTimes(boston, SunTimesMode.Actual, date)
        val civil = service.getSunTimes(boston, SunTimesMode.Civil, date)

        assertTimeNear(LocalTime.of(4, 33), civil.rise)
        assertTrue(civil.rise!!.isBefore(actual.rise))
        assertTrue(civil.set!!.isAfter(actual.set))
    }

    @Test
    fun lengthOfDayOnTheSolstices() {
        val summer = service.getLengthOfDay(boston, SunTimesMode.Actual, LocalDate.of(2025, 6, 21))
        val winter = service.getLengthOfDay(boston, SunTimesMode.Actual, LocalDate.of(2025, 12, 21))

        assertDurationNear(Duration.ofHours(15).plusMinutes(18), summer)
        assertDurationNear(Duration.ofHours(9).plusMinutes(3), winter)
    }

    @Test
    fun theSunIsUpDuringTheDayAndDownAtNight() {
        val noon = ZonedDateTime.of(2025, 6, 21, 12, 0, 0, 0, zone)
        val midnight = ZonedDateTime.of(2025, 6, 21, 0, 0, 0, 0, zone)

        assertTrue(service.isSunUp(boston, noon))
        assertFalse(service.isSunUp(boston, midnight))
    }

    @Test
    fun nextSunriseAndSunsetAreRelativeToTheClock() {
        val beforeSunrise = serviceAt(ZonedDateTime.of(2025, 6, 21, 3, 0, 0, 0, zone))
        val afterSunset = serviceAt(ZonedDateTime.of(2025, 6, 21, 22, 0, 0, 0, zone))

        val sameDaySunrise = beforeSunrise.getNextSunrise(boston, SunTimesMode.Actual)
        val sameDaySunset = beforeSunrise.getNextSunset(boston, SunTimesMode.Actual)
        val nextDaySunrise = afterSunset.getNextSunrise(boston, SunTimesMode.Actual)

        assertEquals(LocalDate.of(2025, 6, 21), sameDaySunrise!!.toLocalDate())
        assertTimeNear(LocalTime.of(5, 7), sameDaySunrise.toLocalTime())
        assertEquals(LocalDate.of(2025, 6, 21), sameDaySunset!!.toLocalDate())
        assertTimeNear(LocalTime.of(20, 25), sameDaySunset.toLocalTime())
        assertEquals(LocalDate.of(2025, 6, 22), nextDaySunrise!!.toLocalDate())
    }

    @Test
    fun todayAndTomorrowSunTimesFollowTheClock() {
        val clocked = serviceAt(ZonedDateTime.of(2025, 6, 21, 12, 0, 0, 0, zone))

        val today = clocked.getTodaySunTimes(boston, SunTimesMode.Actual)
        val tomorrow = clocked.getTomorrowSunTimes(boston, SunTimesMode.Actual)

        assertEquals(LocalDate.of(2025, 6, 21), today.rise!!.toLocalDate())
        assertEquals(LocalDate.of(2025, 6, 22), tomorrow.rise!!.toLocalDate())
    }

    @Test
    fun theSunNeverSetsInTheArcticSummer() {
        val arctic = Coordinate(80.0, 0.0)
        val noon = ZonedDateTime.of(2025, 6, 21, 12, 0, 0, 0, zone)
        val clocked = serviceAt(noon)

        assertTrue(clocked.isSunUp(arctic, noon))
        assertNull(clocked.getNextSunset(arctic, SunTimesMode.Actual))
        assertEquals(
            Duration.ofDays(1),
            service.getLengthOfDay(arctic, SunTimesMode.Actual, LocalDate.of(2025, 6, 21))
        )
    }

    @Test
    fun theSunNeverRisesInTheArcticWinter() {
        val arctic = Coordinate(80.0, 0.0)
        val noon = ZonedDateTime.of(2025, 12, 21, 12, 0, 0, 0, zone)
        val clocked = serviceAt(noon)

        assertFalse(clocked.isSunUp(arctic, noon))
        assertNull(clocked.getNextSunrise(arctic, SunTimesMode.Actual))
        assertEquals(
            Duration.ZERO,
            service.getLengthOfDay(arctic, SunTimesMode.Actual, LocalDate.of(2025, 12, 21))
        )
    }

    @Test
    fun sunAltitudePeaksAtTheExpectedAngleAtSolarNoon() {
        // 90 - latitude + solar declination (23.44 degrees at the June solstice)
        val expectedPeak = 90f - 42.36f + 23.44f

        val altitudes = service.getSunAltitudes(boston, LocalDate.of(2025, 6, 21))

        assertEquals(expectedPeak, altitudes.maxOf { it.value }, 1.5f)
        assertTrue(altitudes.minOf { it.value } < 0f)
    }

    @Test
    fun sunAltitudesCoverTheWholeDay() {
        val altitudes = service.getSunAltitudes(boston, LocalDate.of(2025, 6, 21))

        assertEquals(LocalDate.of(2025, 6, 21), altitudes.first().time.atZone(zone).toLocalDate())
        assertEquals(LocalDate.of(2025, 6, 21), altitudes.last().time.atZone(zone).toLocalDate())
        assertTrue(Duration.between(altitudes.first().time, altitudes.last().time) >= Duration.ofHours(23))
    }

    @Test
    fun centeredAltitudesSpanTwelveHoursEitherSideOfTheTime() {
        val time = ZonedDateTime.of(2025, 6, 21, 12, 0, 0, 0, zone)

        val altitudes = service.getCenteredSunAltitudes(boston, time)

        assertEquals(time.minusHours(12).toInstant(), altitudes.first().time)
        assertEquals(time.plusHours(12).toInstant(), altitudes.last().time)
    }

    @Test
    fun seasonsAreOppositeInEachHemisphere() {
        // The day after each solstice, since seasons are evaluated from the start of the day
        val june = LocalDate.of(2025, 6, 22)
        val december = LocalDate.of(2025, 12, 22)

        assertEquals(Season.Summer, service.getSeason(boston, june))
        assertEquals(Season.Winter, service.getSeason(Coordinate(-33.9, 151.2), june))
        assertEquals(Season.Winter, service.getSeason(boston, december))
        assertEquals(Season.Summer, service.getSeason(Coordinate(-33.9, 151.2), december))
    }

    // MOON

    @Test
    fun moonPhaseNearAKnownFullMoonIsFull() {
        // Full moon: 2025-03-14 06:55 UTC
        val phase = service.getMoonPhase(ZonedDateTime.of(2025, 3, 14, 2, 55, 0, 0, zone))

        assertTrue(phase.illumination > 99f, "illumination was ${phase.illumination}")
    }

    @Test
    fun moonPhaseNearAKnownNewMoonIsDark() {
        // New moon: 2025-03-29 10:58 UTC
        val phase = service.getMoonPhase(ZonedDateTime.of(2025, 3, 29, 6, 58, 0, 0, zone))

        assertTrue(phase.illumination < 1f, "illumination was ${phase.illumination}")
    }

    @Test
    fun moonIsHalfLitAtQuarterMoon() {
        // First quarter: 2025-03-06 16:32 UTC
        val phase = service.getMoonPhase(ZonedDateTime.of(2025, 3, 6, 11, 32, 0, 0, zone))

        assertEquals(50f, phase.illumination, 3f)
    }

    @Test
    fun moonPhaseForADateIsEvaluatedAtNoon() {
        val byDate = service.getMoonPhase(LocalDate.of(2025, 3, 10))
        val atNoon = service.getMoonPhase(ZonedDateTime.of(2025, 3, 10, 12, 0, 0, 0, zone))

        assertEquals(atNoon.illumination, byDate.illumination, 0.01f)
    }

    @Test
    fun superMoonIsDetectedOnAKnownSuperMoon() {
        // Full moon at perigee: 2025-11-05
        assertTrue(service.isSuperMoon(LocalDate.of(2025, 11, 5)))
    }

    @Test
    fun superMoonIsNotReportedWhenTheMoonIsNotFull() {
        assertFalse(service.isSuperMoon(LocalDate.of(2025, 11, 12)))
    }

    @Test
    fun moonRisesAndSetsOnTheSameCalendarDayMostDays() {
        val times = service.getMoonTimes(boston, LocalDate.of(2025, 3, 14))

        // The full moon rises near sunset and sets near sunrise
        assertNotNull(times.rise)
        assertTrue(times.rise!!.hour in 16..20, "moonrise was ${times.rise}")
    }

    // EVENT SEARCH

    @Test
    fun findsTheNextFullMoon() = runBlocking {
        val next = service.findNextEvent(AstronomyEvent.FullMoon, boston, LocalDate.of(2025, 3, 1))

        assertDateNear(LocalDate.of(2025, 3, 14), next)
    }

    @Test
    fun findsTheNextNewMoon() = runBlocking {
        val next = service.findNextEvent(AstronomyEvent.NewMoon, boston, LocalDate.of(2025, 3, 1))

        assertDateNear(LocalDate.of(2025, 3, 29), next)
    }

    @Test
    fun findsTheNextQuarterMoon() = runBlocking {
        val next =
            service.findNextEvent(AstronomyEvent.QuarterMoon, boston, LocalDate.of(2025, 3, 1))

        // First quarter on 2025-03-06
        assertDateNear(LocalDate.of(2025, 3, 6), next)
    }

    @Test
    fun searchSkipsTheEventWhenStartingDuringIt() = runBlocking {
        val fullMoon = service.findNextEvent(AstronomyEvent.FullMoon, boston, LocalDate.of(2025, 3, 1))!!

        val following = service.findNextEvent(AstronomyEvent.FullMoon, boston, fullMoon)

        // Next full moon is 2025-04-13, about 29.5 days later
        assertDateNear(LocalDate.of(2025, 4, 13), following)
    }

    @Test
    fun searchNeverReturnsTheStartDate() = runBlocking {
        val start = LocalDate.of(2025, 3, 14)

        val next = service.findNextEvent(AstronomyEvent.FullMoon, boston, start)

        assertTrue(next!! > start)
    }

    @Test
    fun findsTheNextSupermoon() = runBlocking {
        val next = service.findNextEvent(AstronomyEvent.Supermoon, boston, LocalDate.of(2025, 10, 10))

        assertDateNear(LocalDate.of(2025, 11, 5), next)
    }

    @Test
    fun findsTheNextMeteorShowerPeak() = runBlocking {
        val next =
            service.findNextEvent(AstronomyEvent.MeteorShower, boston, LocalDate.of(2025, 8, 1))

        // Perseids peak 2025-08-12
        assertDateNear(LocalDate.of(2025, 8, 12), next)
    }

    @Test
    fun findsTheNextLunarEclipse() = runBlocking {
        val next =
            service.findNextEvent(AstronomyEvent.LunarEclipse, boston, LocalDate.of(2025, 3, 1))

        // Total lunar eclipse visible from the Americas on 2025-03-14
        assertDateNear(LocalDate.of(2025, 3, 14), next)
    }

    @Test
    fun findsTheNextSolarEclipse() = runBlocking {

        val next =
            service.findNextEvent(AstronomyEvent.SolarEclipse, cleveland, LocalDate.of(2024, 3, 1))

        // Total solar eclipse 2024-04-08
        assertEquals(LocalDate.of(2024, 4, 8), next)
    }

    @Test
    fun searchReturnsNullWhenTheEventIsOutsideOfTheSearchWindow() = runBlocking {
        val next = service.findNextEvent(
            AstronomyEvent.FullMoon,
            boston,
            LocalDate.of(2025, 3, 15),
            maxSearch = Duration.ofDays(10)
        )

        assertNull(next)
    }

    // ECLIPSES

    @Test
    fun reportsTheTotalSolarEclipseOverCleveland() {
        val eclipse = service.getSolarEclipse(cleveland, LocalDate.of(2024, 4, 8))

        assertNotNull(eclipse)
        assertTrue(eclipse!!.isTotal, "magnitude was ${eclipse.magnitude}")
        assertTrue(eclipse.obscuration > 0.99f, "obscuration was ${eclipse.obscuration}")
        // Totality in Cleveland was at 3:13 PM EDT
        assertTimeNear(LocalTime.of(15, 13), eclipse.peak.toLocalTime(), minutes = 10)
        assertTrue(eclipse.start.isBefore(eclipse.peak), "start ${eclipse.start} peak ${eclipse.peak}")
        assertTrue(eclipse.end.isAfter(eclipse.peak), "end ${eclipse.end} peak ${eclipse.peak}")
        assertTrue(eclipse.peakAltitude > 40f, "alt ${eclipse.peakAltitude}")
    }

    @Disabled(
        "Sol reports a magnitude of 0.96 (partial) for Dallas and Kerrville, which were on the " +
                "center line of the 2024-04-08 totality path, and a peak about 5 minutes late"
    )
    @Test
    fun reportsTheTotalSolarEclipseOnTheCenterLine() {
        // Dallas, TX: totality lasted about 3m50s and was centered near 1:42 PM CDT (2:42 PM EDT)
        val eclipse = service.getSolarEclipse(Coordinate(32.78, -96.80), LocalDate.of(2024, 4, 8))

        assertNotNull(eclipse)
        assertTrue(eclipse!!.isTotal, "magnitude was ${eclipse.magnitude}")
        assertTimeNear(LocalTime.of(14, 42), eclipse.peak.toLocalTime(), minutes = 2)
    }

    @Test
    fun reportsNoSolarEclipseOnOrdinaryDays() {
        assertNull(service.getSolarEclipse(cleveland, LocalDate.of(2024, 4, 15)))
    }

    @Test
    fun reportsTheTotalLunarEclipseOverBoston() {
        val eclipse = service.getLunarEclipse(boston, LocalDate.of(2025, 3, 14))

        assertNotNull(eclipse)
        assertTrue(eclipse!!.isTotal)
        // Greatest eclipse: 06:59 UTC (2:59 AM EDT)
        assertTimeNear(LocalTime.of(2, 59), eclipse.peak.toLocalTime(), minutes = 10)
    }

    @Test
    fun reportsNoLunarEclipseOnOrdinaryDays() {
        assertNull(service.getLunarEclipse(boston, LocalDate.of(2025, 3, 25)))
    }

    // METEOR SHOWERS

    @Test
    fun reportsThePerseidsOnTheirPeakDay() {
        val shower = service.getMeteorShower(boston, LocalDate.of(2025, 8, 12))

        assertNotNull(shower)
        assertEquals("Perseids", shower!!.shower.name)
    }

    @Test
    fun reportsNoMeteorShowerOnAQuietDay() {
        assertNull(service.getMeteorShower(boston, LocalDate.of(2025, 6, 1)))
    }

    // HELPERS

    private fun serviceAt(time: ZonedDateTime): AstronomyService {
        return AstronomyService(Clock.fixed(time.toInstant(), zone))
    }

    private fun assertTimeNear(
        expected: LocalTime,
        actual: ZonedDateTime?,
        minutes: Long = 6
    ) {
        assertNotNull(actual)
        assertTimeNear(expected, actual!!.withZoneSameInstant(zone).toLocalTime(), minutes)
    }

    private fun assertTimeNear(expected: LocalTime, actual: LocalTime, minutes: Long = 6) {
        val difference = Math.abs(ChronoUnit.MINUTES.between(expected, actual))
        assertTrue(
            difference <= minutes,
            "Expected $expected (+/- $minutes minutes) but was $actual"
        )
    }

    private fun assertDurationNear(expected: Duration, actual: Duration, minutes: Long = 6) {
        val difference = expected.minus(actual).abs().toMinutes()
        assertTrue(
            difference <= minutes,
            "Expected $expected (+/- $minutes minutes) but was $actual"
        )
    }

    private fun assertDateNear(expected: LocalDate, actual: LocalDate?, days: Long = 1) {
        assertNotNull(actual)
        val difference = Math.abs(ChronoUnit.DAYS.between(expected, actual))
        assertTrue(difference <= days, "Expected $expected (+/- $days days) but was $actual")
    }
}
