package com.kylecorry.trail_sense.tools.tides

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
            losAngeles to tides(
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
            // Diurnal tides
//            perth to tides(
//                LocalDate.of(2024, 12, 3),
//                awst,
//                time(6, 42) to false,
//                time(21, 51) to true
//            ),
//            louisiana to tides(
//                LocalDate.of(2024, 11, 28),
//                cst,
//                time(6, 15) to false,
//                time(20, 33) to true,
//            )
        )

        val deltas = tests.flatMap {
            val harmonics = TideModel.getHarmonics(context, it.first)
            check(harmonics, it.second)
        }.map { it.absoluteValue }

        println("50%: ${Statistics.quantile(deltas, 0.5f).roundToInt()} m")
        println("90%: ${Statistics.quantile(deltas, 0.9f).roundToInt()} m")
        println("Min: ${deltas.minOrNull()?.roundToInt()} m")
        println("Max: ${deltas.maxOrNull()?.roundToInt()} m")
    }

    private val rhodeIsland = Coordinate(41.49008, -71.312796)
    private val louisiana = Coordinate(30.126666667, -89.221666667)
    private val losAngeles = Coordinate(33.7833, -118.4167)
    private val dublin = Coordinate(53.35, -6.22)
    private val hongKong = Coordinate(22.28, 114.18)
    private val perth = Coordinate(-31.98, 115.87)

    private val utc = "UTC"
    private val pst = "America/Los_Angeles"
    private val cst = "America/Chicago"
    private val est = "America/New_York"
    private val hkt = "Asia/Hong_Kong"
    private val awst = "Australia/Perth"

    private fun time(hour: Int, minute: Int): LocalTime {
        return LocalTime.of(hour, minute)
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
        harmonics: List<TidalHarmonic>,
        expected: List<Tide>
    ): List<Float> {
        val calculator = HarmonicWaterLevelCalculator(harmonics)
        val ocean = OceanographyService()
        val start = expected.first().time.atStartOfDay()
        val end = expected.first().time.atEndOfDay()
        val actual = ocean.getTides(calculator, start, end)
        assertEquals(expected.size, actual.size)
        return actual.zip(expected).map {
            check(it.first, it.second)
        }
    }

    private fun check(
        actual: Tide,
        expected: Tide
    ): Float {
        assertEquals(expected.isHigh, actual.isHigh)
        val delta = Duration.between(actual.time, expected.time).seconds / 60f
        assertEquals(0f, delta, 90f)
        return delta
    }

}