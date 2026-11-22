package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import com.kylecorry.sol.math.Vector3
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class VisualGroundOdometryTest {

    private val height = 1.5

    // Walking north-ish while turning slowly, with bob and sway
    private fun truth(t: Double): DoubleArray {
        val heading = 0.15 * t
        val d = 1.2 * t
        return doubleArrayOf(
            d * sin(heading) / 1.0 + 0.02 * sin(2 * PI * 0.95 * t),
            d * cos(heading),
            height + 0.025 * sin(2 * PI * 1.9 * t)
        )
    }

    private fun acceleration(t: Double): Vector3 {
        val h = 1e-3
        val a = truth(t - h)
        val b = truth(t)
        val c = truth(t + h)
        return Vector3(
            ((a[0] + c[0] - 2 * b[0]) / (h * h)).toFloat(),
            ((a[1] + c[1] - 2 * b[1]) / (h * h)).toFloat(),
            ((a[2] + c[2] - 2 * b[2]) / (h * h)).toFloat()
        )
    }

    @Test
    fun tracksAWalkAndMeasuresHeight() {
        val scene = SyntheticScene(240, 320, 56.0)
        val odometry = VisualGroundOdometry(scene.projector, 240, 320)
        val scale = ScaleEstimator()
        val random = Random(11)
        val bias = Vector3(0.05f, -0.04f, 0.03f)

        var t = 0.0
        var nextFrame = 0.0
        var tracking = 0
        var frames = 0
        var last: Vector3? = null
        val duration = 4.0
        while (t < duration) {
            scale.addAcceleration(t, acceleration(t) + bias + noise(random, 0.15f))
            if (t >= nextFrame) {
                val p = truth(t)
                val heading = (0.15 * t).toFloat()
                val rotation = floatArrayOf(heading, 0.6f + 0.03f * sin(2 * PI * 1.9 * t).toFloat(), 0.01f * sin(t).toFloat())
                val reported = floatArrayOf(
                    rotation[0] + gaussian(random) * 0.003f,
                    rotation[1] + gaussian(random) * 0.003f,
                    rotation[2] + gaussian(random) * 0.003f
                )
                val image = scene.render(Vector3(p[0].toFloat(), p[1].toFloat(), p[2].toFloat()), rotation)
                val output = odometry.process(image, reported)
                frames++
                output.position?.let {
                    tracking++
                    last = it
                    scale.addVision(t, it)
                }
                nextFrame += 1 / 15.0
            }
            t += 1 / 200.0
        }

        val estimate = scale.estimate()
        assertNotNull(estimate)
        val p = last!!
        val metric = p * estimate!!.height
        val end = truth(t)
        val start = truth(0.0)
        val travelled = Vector3((end[0] - start[0]).toFloat(), (end[1] - start[1]).toFloat(), (end[2] - start[2]).toFloat())
        val drift = (metric - travelled).magnitude() / travelled.magnitude()
        assertTrue(tracking > frames * 0.9)
        assertTrue(abs(estimate.height - height) / height < 0.05)
        assertTrue(drift < 0.05f)
    }

    @Test
    fun placesGroundThatDropsOrRisesAheadAtItsRealHeight() {
        for (slopeAhead in listOf(-0.15f, 0.15f)) {
            // Flat for 6 m, then a slope; the walk stops 3 m before it
            val scene = SyntheticScene(240, 320, 56.0, 0f, breakNorth = 6f, slopeAfterBreak = slopeAhead)
            val odometry = VisualGroundOdometry(scene.projector, 240, 320)
            val rotation = floatArrayOf(0f, 0.5f, 0f)
            val errors = mutableListOf<Float>()
            var t = 0.0
            while (t < 2.5) {
                val camera = Vector3(0f, (1.2 * t).toFloat(), height.toFloat())
                val output = odometry.process(scene.render(camera, rotation), rotation)
                if (t > 2.0) {
                    for (point in output.points) {
                        // Camera heights to meters, relative to where the walk started
                        val world = (point.world ?: continue) * height.toFloat()
                        if (world.y < 7f || world.y > 12f) continue
                        val truth = scene.groundZ(world.y) - height.toFloat()
                        errors.add(abs(world.z - truth))
                    }
                }
                t += 1 / 15.0
            }
            errors.sort()
            val median = errors[errors.size / 2]
            assertTrue(errors.size > 20)
            assertTrue(median < 0.15f)
        }
    }

    @Test
    fun emptyFlatGroundHasNoPhantomObstacles() {
        val scene = SyntheticScene(240, 320, 56.0)
        val odometry = VisualGroundOdometry(scene.projector, 240, 320)
        val grid = OccupancyGrid()
        val random = Random(21)
        var t = 0.0
        var rotationNoise = floatArrayOf(0f, 0f, 0f)
        while (t < 6.0) {
            val p = truth(t)
            val camera = Vector3(p[0].toFloat(), p[1].toFloat(), p[2].toFloat())
            fun rotationAt(time: Double) = floatArrayOf(
                (0.15 * time).toFloat(),
                0.6f + 0.06f * sin(2 * PI * 1.9 * time).toFloat(),
                0.02f * sin(2 * PI * 0.95 * time).toFloat()
            )
            val rotation = rotationAt(t)
            // The orientation is read when the frame arrives, about 80 ms after it was captured,
            // and the fused sensor's error wanders slowly
            rotationNoise = FloatArray(3) { rotationNoise[it] * 0.9f + gaussian(random) * 0.004f }
            val lagged = rotationAt(t + 0.08)
            val reported = FloatArray(3) { lagged[it] + rotationNoise[it] }
            val output = odometry.process(scene.render(camera, rotation), reported)
            val position = output.position
            if (position != null) {
                grid.add(position * height.toFloat(), output.points.mapNotNull { point -> point.world?.let { point.id to it * height.toFloat() } }.toMap())
            }
            t += 1 / 15.0
        }
        // Everything is flat ground at z = -height, so anything well above it is a phantom
        val voxels = grid.occupied().map { grid.center(it) }
        val phantoms = voxels.count { it.z > -height + 0.45 }
        assertTrue(voxels.size > 30)
        assertTrue(phantoms <= voxels.size * 0.05)
    }

    private fun noise(random: Random, sigma: Float) =
        Vector3(gaussian(random), gaussian(random), gaussian(random)) * sigma

    private fun gaussian(random: Random): Float {
        val u1 = random.nextDouble().coerceAtLeast(1e-9)
        return (sqrt(-2 * ln(u1)) * cos(2 * PI * random.nextDouble())).toFloat()
    }
}
