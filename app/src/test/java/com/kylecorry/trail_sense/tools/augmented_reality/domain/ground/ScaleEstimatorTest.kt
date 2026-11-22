package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import com.kylecorry.sol.math.Vector3
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class ScaleEstimatorTest {

    private class Motion(val name: String, val position: (Double) -> DoubleArray)

    private val walking = Motion("walking") { t ->
        val d = 1.3 * t
        doubleArrayOf(0.02 * sin(2 * PI * 0.95 * t), d, 0.025 * sin(2 * PI * 1.9 * t))
    }
    private val handWave = Motion("hand wave") { t ->
        doubleArrayOf(0.1 * sin(2 * PI * 1.0 * t), 0.02 * sin(2 * PI * 0.7 * t), 0.03 * sin(2 * PI * 1.3 * t))
    }
    private val slowPan = Motion("slow steady move") { t ->
        doubleArrayOf(0.05 * t, 0.0, 0.0)
    }
    private val still = Motion("standing still") { _ -> doubleArrayOf(0.0, 0.0, 0.0) }

    private fun acceleration(motion: Motion, t: Double): Vector3 {
        val h = 1e-3
        val a = motion.position(t - h)
        val b = motion.position(t)
        val c = motion.position(t + h)
        return Vector3(
            ((a[0] + c[0] - 2 * b[0]) / (h * h)).toFloat(),
            ((a[1] + c[1] - 2 * b[1]) / (h * h)).toFloat(),
            ((a[2] + c[2] - 2 * b[2]) / (h * h)).toFloat()
        )
    }

    private fun run(motion: Motion, height: Float, timeOffset: Double, fps: Double, random: Random): ScaleEstimator.Estimate? {
        val estimator = ScaleEstimator()
        val bias = Vector3(0.05f, -0.04f, 0.03f)
        var t = 0.0
        var nextFrame = 0.0
        while (t < 4.0) {
            val tremor = Vector3(gaussian(random), gaussian(random), gaussian(random)) * 0.15f
            estimator.addAcceleration(t, acceleration(motion, t) + bias + tremor)
            if (t >= nextFrame) {
                val p = motion.position(t)
                val noise = Vector3(gaussian(random), gaussian(random), gaussian(random)) * 0.004f
                // Frame timestamps are off from the IMU's by a little
                estimator.addVision(
                    t + timeOffset,
                    Vector3((p[0] / height).toFloat(), (p[1] / height).toFloat(), (p[2] / height).toFloat()) + noise
                )
                nextFrame += 1 / fps + (random.nextDouble() - 0.5) * 0.02
            }
            t += 1 / 200.0
        }
        return estimator.estimate()
    }

    @Test
    fun measuresCameraHeightFromMovement() {
        for (motion in listOf(walking, handWave, slowPan, still)) {
            for ((fps, delay) in listOf(15.0 to 0.0, 15.0 to 0.08, 6.0 to 0.08)) {
                val random = Random(9)
                val errors = mutableListOf<Float>()
                var confident = 0
                repeat(20) {
                    val height = 1.2f + random.nextFloat() * 0.6f
                    val estimate = run(motion, height, delay, fps, random) ?: return@repeat
                    if (estimate.relativeError > 0.1f) return@repeat
                    confident++
                    errors.add(abs(estimate.height - height) / height)
                }
                errors.sort()
                if (motion == walking || motion == handWave) {
                    assertTrue(confident >= 16)
                    assertTrue(errors[errors.size / 2] < 0.06f)
                }
                if (motion == still) assertTrue(confident == 0)
            }
        }
    }

    private fun gaussian(random: Random): Float {
        val u1 = random.nextDouble().coerceAtLeast(1e-9)
        return (sqrt(-2 * ln(u1)) * cos(2 * PI * random.nextDouble())).toFloat()
    }
}
