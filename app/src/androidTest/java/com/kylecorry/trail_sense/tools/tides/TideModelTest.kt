package com.kylecorry.trail_sense.tools.tides

import com.kylecorry.sol.math.optimization.GoldenSearchExtremaFinder
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.science.oceanography.OceanographyService
import com.kylecorry.sol.science.oceanography.TidalHarmonic
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.waterlevel.HarmonicWaterLevelCalculator
import com.kylecorry.sol.time.Time.atEndOfDay
import com.kylecorry.sol.time.Time.atStartOfDay
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.tools.tides.infrastructure.model.TideModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class TideModelTest {

    @Test
    fun testTideModel() = runBlocking {
        val tests = listOf(
            // Semi-diurnal tides
            rhodeIsland to tides(
                LocalDate.of(2024, 11, 25),
                est,
                time(2, 21) to true,
                time(8, 33) to false,
                time(14, 42) to true,
                time(21, 2) to false
            ),
            california to tides(
                LocalDate.of(2024, 11, 25),
                pst,
                time(5, 47) to true,
                time(12, 13) to false,
                time(17, 49) to true,
                time(23, 33) to false
            ),
            dublin to tides(
                LocalDate.of(2024, 11, 25),
                utc,
                time(0, 46) to false,
                time(7, 19) to true,
                time(13, 0) to false,
                time(19, 21) to true
            ),
            hongKong to tides(
                LocalDate.of(2024, 11, 27),
                hkt,
                time(1, 10) to false,
                time(6, 47) to true,
                time(12, 34) to false,
                time(19, 3) to true
            ),
            panama to tides(
                LocalDate.of(2024, 11, 28),
                est,
                time(1, 36) to true,
                time(8, 1) to false,
                time(14, 9) to true,
                time(20, 4) to false
            ),
            hawaii to tides(
                LocalDate.of(2024, 11, 28),
                hawaiiTimeZone,
                time(1, 52) to true,
                time(8, 7) to false,
                time(12, 48) to true,
                time(19, 10) to false
            ),
            fiji to tides(
                LocalDate.of(2024, 11, 25),
                fijiTimeZone,
                time(2, 50) to true,
                time(8, 59) to false,
                time(15, 22) to true,
                time(21, 40) to false
            ),
            southAfrica to tides(
                LocalDate.of(2024, 11, 26),
                sast,
                time(0, 22) to true,
                time(6, 13) to false,
                time(12, 29) to true,
                time(18, 54) to false
            ),
            italy to tides(
                LocalDate.of(2024, 11, 28),
                cet,
                time(1, 2) to false,
                time(7, 19) to true,
                time(13, 11) to false,
                time(19, 9) to true
            ),
            alaska to tides(
                LocalDate.of(2024, 11, 27),
                akst,
                time(4, 59) to true,
                time(10, 52) to false,
                time(16, 39) to true,
                time(23, 40) to false
            ),
            india to tides(
                LocalDate.of(2024, 11, 29),
                ist,
                time(1, 17) to false,
                time(6, 57) to true,
                time(13, 6) to false,
                time(19, 42) to true
            ),
            brazil to tides(
                LocalDate.of(2024, 11, 30),
                "America/Sao_Paulo",
                time(5, 50) to false,
                time(10, 38) to true,
                time(17, 57) to false,
                time(22, 50) to true
            ),
            chile to tides(
                LocalDate.of(2024, 11, 30),
                "America/Santiago",
                time(4, 38) to false,
                time(10, 15) to true,
                time(15, 57) to false,
                time(22, 32) to true
            ),
            nigeria to tides(
                date(2024, 11, 30),
                "Africa/Lagos",
                high(3, 37),
                low(9, 54),
                high(4, 23, true),
                low(9, 55, true)
            ),
            madagascar to tides(
                date(2024, 11, 30),
                "Indian/Antananarivo",
                low(4, 38),
                high(11, 0),
                low(5, 23, true),
                high(11, 27, true)
            ),
            egypt to tides(
                date(2024, 11, 30),
                "Africa/Cairo",
                high(5, 20),
                low(11, 29),
                high(5, 23, true),
                low(11, 47, true)
            ),
            bulgaria to tides(
                date(2024, 11, 30),
                "Europe/Sofia",
                high(2, 30),
                low(8, 42),
                high(1, 45, true),
                low(8, 2, true)
            ),
            canada to tides(
                date(2024, 11, 30),
                est,
                high(2, 24),
                low(8, 47),
                high(2, 50, true),
                low(9, 8, true)
            ),
            greenland to tides(
                date(2024, 11, 30),
                "UTC-3",
                low(5, 41),
                high(12, 6),
                low(5, 56, true),
                high(11, 47, true)
            ),
            // Mixed tides
            russia to tides(
                LocalDate.of(2024, 11, 26),
                penzhinaBayTimeZone,
                time(3, 35) to true,
                time(11, 48) to false
            ),
            antarctica to tides(
                LocalDate.of(2024, 12, 1),
                antarcticaTimeZone,
                time(2, 2) to true,
                time(10, 44) to false
            ),
            indonesia to tides(
                LocalDate.of(2024, 11, 26),
                wit,
                time(4, 47) to false,
                time(10, 50) to true,
                time(16, 18) to false,
                time(22, 39) to true
            ),
            mexico to tides(
                LocalDate.of(2024, 11, 25),
                cst,
                time(5, 18) to false,
                time(11, 51) to true,
                time(16, 47) to false,
                time(22, 13) to true
            ),
            japan to tides(
                date(2024, 11, 30),
                "Asia/Tokyo",
                low(0, 55),
                high(7, 21),
                low(1, 10, true),
                high(6, 47, true)
            ),
            // Diurnal tides
            perth to tides(
                LocalDate.of(2024, 12, 3),
                awst,
                time(6, 42) to false,
                time(21, 51) to true
            ),
            louisiana to tides(
                LocalDate.of(2024, 11, 26),
                cst,
                time(4, 58) to false,
                time(18, 28) to true,
            ),
            dominicanRepublic to tides(
                date(2024, 11, 30),
                "America/Santo_Domingo",
                low(0, 14),
                high(10, 26)
            ),
            // New moon
            rhodeIsland to tides(
                LocalDate.of(2024, 12, 1),
                est,
                time(0, 24) to false,
                time(7, 22) to true,
                time(13, 27) to false,
                time(19, 44) to true
            ),
            // Full moon
            rhodeIsland to tides(
                LocalDate.of(2024, 12, 15),
                est,
                time(0, 5) to false,
                time(7, 19) to true,
                time(13, 39) to false,
                time(19, 43) to true
            ),
            // First quarter
            rhodeIsland to tides(
                LocalDate.of(2024, 12, 8),
                est,
                time(0, 40) to true,
                time(6, 0) to false,
                time(13, 1) to true,
                time(18, 44) to false
            ),
            // Third quarter
            rhodeIsland to tides(
                LocalDate.of(2024, 12, 22),
                est,
                time(0, 46) to true,
                time(5, 58) to false,
                time(13, 0) to true,
                time(18, 33) to false
            )
        )

        val errors = tests.flatMapIndexed { index, test ->
//            if (index != 16){
//                return@flatMapIndexed listOf(0f)
//            }
            val harmonics = TideModel.getHarmonics(context, test.first)
            check(index, harmonics, test.second)
        }

        val absoluteErrors = errors.map { it.absoluteValue }

        println("50%: ${Statistics.quantile(absoluteErrors, 0.5f).roundToInt()} m")
        println("90%: ${Statistics.quantile(absoluteErrors, 0.9f).roundToInt()} m")
        println("Min: ${absoluteErrors.minOrNull()?.roundToInt()} m")
        println("Max: ${absoluteErrors.maxOrNull()?.roundToInt()} m")

        // Check the average error and standard deviation
        val absAverageError = Statistics.mean(absoluteErrors)
        val abs90QuantileError = Statistics.quantile(absoluteErrors, 0.9f)

        assertEquals("Average", 0f, absAverageError, 30f)
        assertEquals("90% Quantile", 0f, abs90QuantileError, 45f)
    }

    private val rhodeIsland = Coordinate(41.49008, -71.312796)
    private val louisiana = Coordinate(29.06, -90.96)
    private val california = Coordinate(33.7833, -118.4167)
    private val dublin = Coordinate(53.35, -6.22)
    private val hongKong = Coordinate(22.28, 114.18)
    private val perth = Coordinate(-31.98, 115.87)
    private val panama = Coordinate(8.27, -82.85)
    private val antarctica = Coordinate(-68.0, 78.5)
    private val hawaii = Coordinate(19.73, -155.06)
    private val fiji = Coordinate(-18.13, 178.43)
    private val russia = Coordinate(62.38, 164.50)
    private val indonesia = Coordinate(-3.68, 128.18)
    private val mexico = Coordinate(22.22, -97.86)
    private val southAfrica = Coordinate(-33.9, 18.42)
    private val italy = Coordinate(40.86, 14.28)
    private val alaska = Coordinate(61.24, -149.89)
    private val india = Coordinate(13.10, 80.30)
    private val brazil = Coordinate(-1.51, -48.63)
    private val chile = Coordinate(-27.07, -70.83)
    private val nigeria = Coordinate(4.30, 6.24)
    private val madagascar = Coordinate(-22.13, 48.02)
    private val egypt = Coordinate(27.24, 33.84)
    private val bulgaria = Coordinate(43.22, 27.95)
    private val japan = Coordinate(31.85, 130.22)
    private val canada = Coordinate(55.28, -85.10)
    private val dominicanRepublic = Coordinate(18.45, -69.61)
    private val greenland = Coordinate( 81.07, -61.08)

    private val utc = "UTC"
    private val pst = "America/Los_Angeles"
    private val cst = "America/Chicago"
    private val est = "America/New_York"
    private val hkt = "Asia/Hong_Kong"
    private val awst = "Australia/Perth"
    private val antarcticaTimeZone = "UTC+7"
    private val hawaiiTimeZone = "Pacific/Honolulu"
    private val fijiTimeZone = "Pacific/Fiji"
    private val penzhinaBayTimeZone = "Asia/Kamchatka"
    private val wit = "Asia/Jayapura"
    private val sast = "Africa/Johannesburg"
    private val cet = "CET"
    private val akst = "America/Anchorage"
    private val ist = "Asia/Kolkata"

    private fun low(hour: Int, minute: Int, add12Hours: Boolean = false): Pair<LocalTime, Boolean> {
        return time(hour + if (add12Hours) 12 else 0, minute) to false
    }

    private fun high(
        hour: Int,
        minute: Int,
        add12Hours: Boolean = false
    ): Pair<LocalTime, Boolean> {
        return time(hour + if (add12Hours) 12 else 0, minute) to true
    }

    private fun time(hour: Int, minute: Int): LocalTime {
        return LocalTime.of(hour, minute)
    }

    private fun date(year: Int, month: Int, day: Int): LocalDate {
        return LocalDate.of(year, month, day)
    }

    private fun tides(
        date: LocalDate,
        zoneId: String,
        vararg tides: Pair<LocalTime, Boolean>
    ): List<Tide> {
        return tides.map {
            Tide(ZonedDateTime.of(date, it.first, ZoneId.of(zoneId)), it.second)
        }
    }

    private fun check(
        index: Int,
        harmonics: List<TidalHarmonic>,
        expected: List<Tide>
    ): List<Float> {
        val calculator = HarmonicWaterLevelCalculator(harmonics)
        val ocean = OceanographyService()
        val start = expected.first().time.atStartOfDay().minusHours(12)
        val end = expected.first().time.atEndOfDay().plusHours(12)
        val actual = ocean.getTides(calculator, start, end, GoldenSearchExtremaFinder(30.0, 1.0))
        // Match each tide
        val matchedTides = expected.map {
            val closest = actual.minBy { actualTide ->
                Duration.between(actualTide.time, it.time).abs()
            }
            closest
        }

        // Verify there are no tides between the first matched tide and the last matched tide that are not matched
        val firstIdx = actual.indexOf(matchedTides.first())
        for (i in firstIdx until (firstIdx + matchedTides.size)) {
            assertEquals(index.toString(), matchedTides[i - firstIdx], actual[i])
        }

        return matchedTides.zip(expected).map {
            check(index, it.first, it.second)
        }
    }

    private fun check(
        index: Int,
        actual: Tide,
        expected: Tide
    ): Float {
        assertEquals(index.toString(), expected.isHigh, actual.isHigh)
        val delta = Duration.between(actual.time, expected.time).seconds / 60f
        assertEquals(index.toString(), 0f, delta, 90f)
        return delta
    }

}