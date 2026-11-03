package com.kylecorry.trail_sense.tools.tides

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.sol.math.optimization.GoldenSearchExtremaFinder
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.science.oceanography.Oceanography
import com.kylecorry.sol.science.oceanography.waterlevel.HarmonicWaterLevelCalculator
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.tides.infrastructure.model.TideModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.absoluteValue

/**
 * Compares the tide model against the high and low tide times NOAA predicts (March 15-17, 2025, GMT) at 70 harmonic
 * reference stations spread across the US coasts, Alaska, Hawaii, and the Pacific. Many of the stations are in bays
 * and estuaries, where a global model is least accurate, so the errors are larger than for open coast.
 *
 * Each row of noaa_tide_predictions.csv is: station ID, latitude, longitude, H (high) or L (low), time.
 */
class TideModelNoaaTest {

    private class Station(val location: Coordinate, val tides: List<Pair<Boolean, LocalDateTime>>)

    @Test
    fun matchesNoaaPredictions() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        DependencyRegistry.addSingleton(FileSubsystem.getInstance(context))

        val stations = loadStations(instrumentation.context.assets.open("noaa_tide_predictions.csv").bufferedReader().readLines())

        // Errors in minutes, or 6 hours if the model has no tide for the station or no tide of that type nearby
        val errors = stations.flatMap { station ->
            val harmonics = TideModel.getHarmonics(context, station.location)?.harmonics
                ?: return@flatMap station.tides.map { NO_PREDICTION_MINUTES }
            val times = station.tides.map { it.second.atZone(ZoneOffset.UTC) }
            val predicted = Oceanography.getTides(
                HarmonicWaterLevelCalculator(harmonics),
                times.first().minusHours(12),
                times.last().plusHours(12),
                GoldenSearchExtremaFinder(30.0, 1.0)
            )
            station.tides.mapIndexed { i, (isHigh, _) ->
                val closest = predicted.filter { it.isHigh == isHigh }
                    .minByOrNull { Duration.between(it.time, times[i]).abs() }
                closest?.let { Duration.between(it.time, times[i]).seconds / 60f }
                    ?: NO_PREDICTION_MINUTES
            }
        }

        val absoluteErrors = errors.map { it.absoluteValue }
        val average = Statistics.mean(absoluteErrors)
        val median = Statistics.quantile(absoluteErrors, 0.5f)
        val quantile90 = Statistics.quantile(absoluteErrors, 0.9f)
        val within30Minutes = absoluteErrors.count { it <= 30 } / absoluteErrors.size.toFloat()

        println("Stations: ${stations.size}, tides: ${errors.size}")
        println("Average: $average min")
        println("50%: $median min")
        println("90%: $quantile90 min")
        println("Within 30 minutes: ${within30Minutes * 100}%")

        assertEquals("Average", 0f, average, MAX_AVERAGE_MINUTES)
        assertEquals("50% Quantile", 0f, median, MAX_MEDIAN_MINUTES)
        assertEquals("90% Quantile", 0f, quantile90, MAX_QUANTILE_90_MINUTES)
        assertTrue("Within 30 minutes: $within30Minutes", within30Minutes >= MIN_WITHIN_30_MINUTES)
    }

    private fun loadStations(lines: List<String>): List<Station> {
        return lines.filter { it.isNotBlank() }
            .map { it.split(",") }
            .groupBy { it[0] }
            .values
            .map { rows ->
                Station(
                    Coordinate(rows[0][1].toDouble(), rows[0][2].toDouble()),
                    rows.map { (it[3] == "H") to LocalDateTime.parse(it[4]) }
                )
            }
    }

    private companion object {
        const val NO_PREDICTION_MINUTES = 360f
        const val MAX_AVERAGE_MINUTES = 59f
        const val MAX_MEDIAN_MINUTES = 15f
        const val MAX_QUANTILE_90_MINUTES = 200f
        const val MIN_WITHIN_30_MINUTES = 0.62f
    }
}
