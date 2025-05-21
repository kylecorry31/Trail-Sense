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
                date(2024, 11, 25),
                "-05:00",
                high(2, 21),
                low(8, 33),
                high(14, 42),
                low(21, 2)
            ),
            california to tides(
                date(2024, 11, 25),
                "-08:00",
                high(5, 47),
                low(12, 13),
                high(17, 49),
                low(23, 33)
            ),
            dublin to tides(
                date(2024, 11, 25),
                "+00:00",
                low(0, 46),
                high(7, 19),
                low(13, 0),
                high(19, 21)
            ),
            hongKong to tides(
                date(2024, 11, 27),
                "+08:00",
                low(1, 10),
                high(6, 47),
                low(12, 34),
                high(19, 3)
            ),
            panama to tides(
                date(2024, 11, 28),
                "-05:00",
                high(1, 36),
                low(8, 1),
                high(14, 9),
                low(20, 4)
            ),
            hawaii to tides(
                date(2024, 11, 28),
                "-10:00",
                high(1, 52),
                low(8, 7),
                high(12, 48),
                low(19, 10)
            ),
            fiji to tides(
                date(2024, 11, 25),
                "+12:00",
                high(1, 51),
                low(8, 6),
                high(14, 30),
                low(20, 45)
            ),
            southAfrica to tides(
                date(2024, 11, 26),
                "+02:00",
                high(0, 22),
                low(6, 13),
                high(12, 29),
                low(18, 54)
            ),
            italy to tides(
                date(2024, 11, 28),
                "+01:00",
                low(1, 2),
                high(7, 19),
                low(13, 11),
                high(19, 9)
            ),
            alaska to tides(date(2024, 11, 27), "-09:00", low(5, 30), high(11, 43), low(18, 42)),
            india to tides(
                date(2024, 11, 29),
                "+05:30",
                low(1, 17),
                high(6, 57),
                low(13, 6),
                high(19, 42)
            ),
            brazil to tides(
                date(2024, 11, 30),
                "-02:00",
                low(5, 50),
                high(10, 38),
                low(17, 57),
                high(22, 50)
            ),
            chile to tides(
                date(2024, 11, 30),
                "-03:00",
                low(4, 38),
                high(10, 15),
                low(15, 57),
                high(22, 32)
            ),
            nigeria to tides(
                date(2024, 11, 30),
                "+01:00",
                high(3, 37),
                low(9, 54),
                high(16, 23),
                low(21, 55)
            ),
            madagascar to tides(
                date(2024, 11, 30),
                "+03:00",
                low(4, 38),
                high(11, 0),
                low(17, 23),
                high(23, 27)
            ),
            egypt to tides(
                date(2024, 11, 30),
                "+02:00",
                high(5, 20),
                low(11, 29),
                high(17, 23),
                low(23, 47)
            ),
            bulgaria to tides(
                date(2024, 11, 30),
                "+02:00",
                high(2, 30),
                low(8, 42),
                high(13, 45),
                low(20, 2)
            ),
            canada to tides(
                date(2024, 11, 30),
                "-05:00",
                high(2, 24),
                low(8, 47),
                high(14, 50),
                low(21, 8)
            ),
            greenland to tides(
                date(2024, 11, 30),
                "-02:00",
                low(0, 44),
                high(7, 9),
                low(13, 26),
                high(19, 17)
            ),
            // Mixed tides
            russia to tides(date(2024, 11, 26), "+12:00", high(3, 35), low(11, 48)),
            antarctica to tides(date(2024, 12, 1), "+07:00", high(2, 2), low(10, 44)),
            indonesia to tides(
                date(2024, 11, 26),
                "+09:00",
                low(4, 47),
                high(10, 50),
                low(16, 18),
                high(22, 39)
            ),
            mexico to tides(
                date(2024, 11, 25),
                "-06:00",
                low(5, 18),
                high(11, 51),
                low(16, 47),
                high(22, 13)
            ),
            japan to tides(
                date(2024, 11, 30),
                "+09:00",
                low(0, 55),
                high(7, 21),
                low(13, 10),
                high(18, 47)
            ),
            // Diurnal tides
            perth to tides(date(2024, 12, 3), "+08:00", low(6, 42), high(21, 51)),
            louisiana to tides(date(2024, 11, 26), "-06:00", low(4, 58), high(18, 28)),
            dominicanRepublic to tides(date(2024, 11, 30), "-04:00", low(0, 14), high(10, 26)),
            // New moon
            rhodeIsland to tides(
                date(2024, 12, 1),
                "-05:00",
                low(0, 24),
                high(7, 22),
                low(13, 27),
                high(19, 44)
            ),
            // Full moon
            rhodeIsland to tides(
                date(2024, 12, 15),
                "-05:00",
                low(0, 5),
                high(7, 19),
                low(13, 39),
                high(19, 43)
            ),
            // First quarter
            rhodeIsland to tides(
                date(2024, 12, 8),
                "-05:00",
                high(0, 40),
                low(6, 0),
                high(13, 1),
                low(18, 44)
            ),
            // Third quarter
            rhodeIsland to tides(
                date(2024, 12, 22),
                "-05:00",
                high(0, 46),
                low(5, 58),
                high(13, 0),
                low(18, 33)
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

        assertEquals("Average", 0f, absAverageError, 20f)
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
    private val alaska = Coordinate(56.30, -158.40)
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
    private val greenland = Coordinate(63.08, -50.68)

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