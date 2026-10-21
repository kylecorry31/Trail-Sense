package com.kylecorry.trail_sense.tools.weather.domain.sealevel

import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.Random
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class AutoLoessPocTest {

    // Passes the pressure through unchanged
    private object Identity : ISeaLevelCalibrationStrategy {
        override fun calibrate(readings: List<Reading<RawWeatherObservation>>): List<Reading<Pressure>> =
            readings.map { Reading(Pressure.hpa(it.value.pressure), it.time) }
    }

    private fun obs(p: Float) = RawWeatherObservation(0, p, 0f, 0f)

    private fun rmse(a: List<Float>, b: List<Float>) =
        sqrt(a.zip(b).sumOf { (x, y) -> ((x - y) * (x - y)).toDouble() } / a.size).toFloat()

    private fun series(): Map<String, List<Float>> {
        val csv = File("../scripts/experiments/weather_test_data.csv").readLines().drop(1)
            .map { it.split(",")[12].toFloat() }
        // Synthetic: a front passing through (sigmoid drop) over 3 days
        val front = (0 until 72).map { 1015f - 8f / (1 + Math.exp(-(it - 36) / 4.0).toFloat()) }
        val diurnal = (0 until 72).map { 1013f + 1.5f * sin(2 * Math.PI * it / 12).toFloat() + it * 0.05f }
        return mapOf("csv" to csv, "front" to front, "diurnal" to diurnal)
    }

    @Test
    fun compare() {
        val start = Instant.parse("2025-10-03T00:00:00Z")
        val sb = StringBuilder()
        // Sample intervals the weather monitor could produce: 60, 30, 10 min
        for (stepMin in listOf(60, 30, 10)) {
            sb.appendLine("--- one reading every $stepMin min (RMSE vs clean pressure, hPa) ---")
            sb.appendLine("%-14s %-6s %-8s %-8s %-8s %-8s %-8s %-8s".format("series", "noise", "raw", "fix5%", "fix15%", "fix30%", "bestfix", "AUTO"))
            for ((name, base) in series()) {
                // resample hourly series to the interval by linear interpolation
                val per = 60 / stepMin
                val clean = (0 until (base.size - 1) * per).map { i ->
                    val k = i / per
                    val f = (i % per) / per.toFloat()
                    base[k] + (base[k + 1] - base[k]) * f
                }
                for (noise in listOf(0f, 0.1f, 0.3f, 0.8f)) {
                    val rnd = Random(3)
                    val noisy = clean.map { it + (rnd.nextGaussian() * noise).toFloat() }
                    val readings = noisy.mapIndexed { i, p ->
                        Reading(obs(p), start.plus(Duration.ofMinutes((i * stepMin).toLong())))
                    }
                    fun fixed(s: Float) = DataUtils.smoothTemporal(
                        readings.map { Reading(it.value.pressure, it.time) }, s
                    ).map { it.value }
                    val fixes = listOf(0.05f, 0.15f, 0.3f).map { rmse(fixed(it), clean) }
                    val best = listOf(0.02f, 0.05f, 0.08f, 0.1f, 0.15f, 0.2f, 0.3f, 0.5f).minOf { rmse(fixed(it), clean) }
                    val auto = AutoLoessSeaLevelCalibrationStrategy(Identity).calibrate(readings).map { it.value.value }
                    sb.appendLine(
                        "%-14s %-6.1f %-8.3f %-8.3f %-8.3f %-8.3f %-8.3f %-8.3f".format(
                            name, noise, rmse(noisy, clean), fixes[0], fixes[1], fixes[2], best, rmse(auto, clean)
                        )
                    )
                }
            }
        }
        println(sb)
    }
}
