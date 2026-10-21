package com.kylecorry.trail_sense.shared.sensors.compass

import com.kylecorry.andromeda.sense.orientation.filter.LowPassOrientationSensorFilter
import com.kylecorry.andromeda.sense.orientation.filter.OrientationSensorFilter
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.math.Quaternion
import org.junit.jupiter.api.Test
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class AdaptiveOrientationSensorFilterPocTest {

    private val hz = 50
    private val dt = 1f / hz

    private fun yaw(deg: Float): FloatArray {
        val h = Math.toRadians(deg.toDouble()).toFloat() / 2
        return floatArrayOf(0f, 0f, sin(h), cos(h))
    }

    private fun noisy(q: FloatArray, sigmaDeg: Float, rnd: Random): FloatArray {
        val s = Math.toRadians(sigmaDeg.toDouble()).toFloat() / 2
        val n = Quaternion(
            (rnd.nextGaussian() * s).toFloat(),
            (rnd.nextGaussian() * s).toFloat(),
            (rnd.nextGaussian() * s).toFloat(),
            1f
        ).normalize()
        return (Quaternion.from(q) * n).normalize().toFloatArray()
    }

    private fun angleDeg(a: FloatArray, b: FloatArray): Float {
        val d = abs(a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3]).coerceIn(0f, 1f)
        return Math.toDegrees(2.0 * acos(d.toDouble())).toFloat()
    }

    private class Scenario(
        val name: String,
        val seconds: Float,
        val sigma: Float,
        val heading: (Float) -> Float,
        // Time window where only jitter matters (the device is still)
        val stillRange: ClosedFloatingPointRange<Float>? = null,
        val stepAt: Float? = null
    )

    private val scenarios = listOf(
        Scenario("still s=1", 10f, 1f, { 40f }, 0f..10f),
        Scenario("still s=3", 10f, 3f, { 40f }, 0f..10f),
        Scenario("turn 30dps s=1", 10f, 1f, { 40f + 30f * it }),
        Scenario("drift 3dps s=1", 10f, 1f, { 40f + 3f * it }),
        Scenario("step 90deg s=1", 10f, 1f, { if (it < 5f) 40f else 130f }, 0f..5f, 5f),
        Scenario("wobble 1Hz 10deg s=1", 10f, 1f, { 40f + 10f * sin(2 * PI.toFloat() * it) }),
        // Walk: slow base heading with 2 Hz, 4 deg sway
        Scenario("walking sway s=2", 10f, 2f, { 40f + 4f * sin(4 * PI.toFloat() * it) }, 0f..10f)
    )

    private data class Result(val rms: Float, val still: Float?, val settle: Float?)

    private fun run(s: Scenario, filter: OrientationSensorFilter?, clock: LongArray): Result {
        val rnd = Random(7)
        var t = 0f
        var sumSq = 0.0
        var count = 0
        var stillSq = 0.0
        var stillCount = 0
        var lastBad = -1f
        val out = FloatArray(4)
        var first = true
        while (t < s.seconds) {
            clock[0] = (t * 1e9).toLong()
            val truth = yaw(s.heading(t))
            val raw = noisy(truth, s.sigma, rnd)
            val value = if (filter == null) {
                raw
            } else if (first) {
                filter.reset(raw)
                raw.copyInto(out)
                out
            } else {
                filter.filter(raw, out)
                out
            }
            first = false
            val err = angleDeg(value, truth)
            sumSq += err * err
            count++
            if (s.stillRange != null && t in s.stillRange && t > 1f) {
                stillSq += err * err
                stillCount++
            }
            if (s.stepAt != null && t >= s.stepAt && err > 3f) lastBad = t
            t += dt
        }
        return Result(
            sqrt(sumSq / count).toFloat(),
            if (stillCount > 0) sqrt(stillSq / stillCount).toFloat() else null,
            if (s.stepAt != null) lastBad - s.stepAt else null
        )
    }

    @Test
    fun compare() {
        val clock = LongArray(1)
        fun sliderAlpha(slider: Int) =
            Interpolation.map((1 - slider / 100f).pow(2), 0f, 1f, 0.005f, 1f)

        val filters = linkedMapOf<String, () -> OrientationSensorFilter?>(
            "raw" to { null },
            "slider 1(off)" to { null },
            "slider 25" to { LowPassOrientationSensorFilter(sliderAlpha(25), true) },
            "slider 50" to { LowPassOrientationSensorFilter(sliderAlpha(50), true) },
            "slider 75" to { LowPassOrientationSensorFilter(sliderAlpha(75), true) },
            "slider 90" to { LowPassOrientationSensorFilter(sliderAlpha(90), true) },
            "adaptive" to { AdaptiveOrientationSensorFilter(nanoTime = { clock[0] }) }
        )
        filters.remove("slider 1(off)")

        val sb = StringBuilder()
        sb.appendLine("RMS error deg (still-only RMS) [settle s after step to <3deg]")
        sb.append("%-24s".format("scenario"))
        filters.keys.forEach { sb.append("%-22s".format(it)) }
        sb.appendLine()
        for (s in scenarios) {
            sb.append("%-24s".format(s.name))
            for ((_, make) in filters) {
                val r = run(s, make(), clock)
                val extra = buildString {
                    if (r.still != null) append(" (%.2f)".format(r.still))
                    if (r.settle != null) append(" [%.2f]".format(r.settle))
                }
                sb.append("%-22s".format("%.2f".format(r.rms) + extra))
            }
            sb.appendLine()
        }
        println(sb)
    }

    @Test
    fun sweep() {
        val clock = LongArray(1)
        val sb = StringBuilder("minCut beta speedCut | " + scenarios.joinToString(" | ") { it.name } + "\n")
        for (speedCut in listOf(0.5f, 1f, 2f)) for (minCut in listOf(0.1f, 0.2f, 0.4f)) for (beta in listOf(0.02f, 0.05f, 0.1f, 0.2f)) {
            sb.append("%.2f %.2f %.1f |".format(minCut, beta, speedCut))
            for (sc in scenarios) {
                val r = run(sc, AdaptiveOrientationSensorFilter(minCut, beta, speedCut) { clock[0] }, clock)
                sb.append(" %.2f".format(r.rms))
                if (r.settle != null) sb.append("[%.2f]".format(r.settle))
                sb.append(" |")
            }
            sb.appendLine()
        }
        println(sb)
    }
}
