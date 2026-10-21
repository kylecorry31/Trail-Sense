package com.kylecorry.trail_sense.tools.experimentation

import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Random
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class DeadReckonerSimulationTest {

    private data class Scenario(
        val name: String,
        val strideBias: Float,      // per-walker stride length error (1 sigma, fraction)
        val strideNoise: Float,     // per-step stride error (1 sigma, fraction)
        val stepMiscount: Float,    // fraction of steps missed/extra (1 sigma)
        val headingBias: Float,     // constant compass error (1 sigma, deg)
        val headingJitter: Float,   // per-step compass noise (1 sigma, deg)
        val anomaly: Float,         // magnetic anomaly sigma (deg), changes every ~30 m
        val gyroDriftPerMin: Float = 0f // if > 0, heading is gyro-only: random walk instead of compass
    )

    private val scenarios = listOf(
        Scenario("A good compass, no rock distortion", 0.05f, 0.05f, 0.02f, 3f, 5f, 0f),
        Scenario("B typical compass, mild distortion", 0.08f, 0.05f, 0.03f, 5f, 8f, 10f),
        Scenario("C strong rock distortion", 0.08f, 0.05f, 0.03f, 10f, 8f, 30f),
        Scenario("D gyro only (1 deg/min drift)", 0.08f, 0.05f, 0.03f, 5f, 3f, 0f, 1f),
        Scenario("E gyro only (3 deg/min drift)", 0.08f, 0.05f, 0.03f, 5f, 3f, 0f, 3f),
    )

    @Test
    fun simulate() {
        val out = StringBuilder()
        for (totalMeters in listOf(200f, 500f, 1000f)) {
            out.appendLine("=== Out and back, ${totalMeters.toInt()} m each way ===")
            for (s in scenarios) {
                val errs = (0 until 2000).map { run(Random(it.toLong()), s, totalMeters) }
                val rel = errs.map { it / (totalMeters * 2) * 100 }.sorted()
                out.appendLine(
                    "%-40s median %5.1f%%  p90 %5.1f%%  (median %5.1f m, p90 %5.1f m return error)".format(
                        s.name, rel[rel.size / 2], rel[rel.size * 9 / 10],
                        errs.sorted()[errs.size / 2], errs.sorted()[errs.size * 9 / 10]
                    )
                )
            }
        }
        out.appendLine()
        out.appendLine("=== Barometer vertical error, 2 hour trip ===")
        val v = (0 until 2000).map { baro(Random(it.toLong()), 120) }.sorted()
        out.appendLine("median %.1f m  p90 %.1f m".format(v[v.size / 2], v[v.size * 9 / 10]))
        File("/tmp/claude-1000/-home-kyle-Documents-development-Trail-Sense/55ac90b9-dfb2-4ff9-bdd5-93f29384b814/scratchpad/sim.txt").writeText(out.toString())
    }

    // Returns the distance error (m) between the estimated and true position at the end of the walk
    // (the start) after walking a random passage out and then retracing it back.
    private fun run(r: Random, s: Scenario, meters: Float): Float {
        val stride = 0.7f
        val trueStride = stride * (1 + s.strideBias * r.nextGaussian().toFloat())
        val reckoner = DeadReckoner(stride)
        reckoner.reset(Coordinate.zero)

        // Random passage: heading changes every ~20 m
        val headings = mutableListOf<Float>()
        var h = r.nextFloat() * 360f
        val stepsOut = (meters / trueStride).toInt()
        for (i in 0 until stepsOut) {
            if (i % 28 == 0) h += (r.nextGaussian() * 50).toFloat()
            headings.add(h)
        }
        val all = headings + headings.reversed().map { it + 180f }

        val bias = s.headingBias * r.nextGaussian().toFloat()
        var anomaly = 0f
        var gyroDrift: Float
        val gyroRate = s.gyroDriftPerMin * r.nextGaussian().toFloat()
        var trueX = 0.0
        var trueY = 0.0
        for ((i, trueHeading) in all.withIndex()) {
            if (i % 40 == 0) anomaly = s.anomaly * r.nextGaussian().toFloat()
            val measured = if (s.gyroDriftPerMin > 0) {
                // Linear drift from an uncorrected gyro bias, ~1 step per 0.6 s
                gyroDrift = gyroRate * (i * 0.6f / 60f)
                trueHeading + bias + gyroDrift + s.headingJitter * r.nextGaussian().toFloat()
            } else {
                trueHeading + bias + anomaly + s.headingJitter * r.nextGaussian().toFloat()
            }
            // The compass is sampled several times per step; the average is what the reckoner uses
            reckoner.addHeading(measured)

            val ts = trueStride * (1 + s.strideNoise * r.nextGaussian().toFloat())
            trueX += ts * sin(Math.toRadians(trueHeading.toDouble()))
            trueY += ts * cos(Math.toRadians(trueHeading.toDouble()))

            // Counted steps differ slightly from real steps
            if (r.nextFloat() > s.stepMiscount * 3) reckoner.addSteps(1)
        }
        val p = reckoner.position
        return hypot(p.east - trueX, p.north - trueY).toFloat()
    }

    // Pressure sensor noise 0.05 hPa, plus weather drift of up to ~0.5 hPa/hour
    private fun baro(r: Random, minutes: Int): Float {
        val reckoner = DeadReckoner(0.7f)
        reckoner.reset(Coordinate.zero)
        val p0 = 1000f
        val drift = 0.5f / 60f * r.nextGaussian().toFloat()
        for (m in 0..minutes) {
            reckoner.addPressure(p0 + drift * m + 0.05f * r.nextGaussian().toFloat())
        }
        return abs(reckoner.position.up)
    }
}
