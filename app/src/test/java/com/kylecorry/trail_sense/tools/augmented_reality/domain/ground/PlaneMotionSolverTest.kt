package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import com.kylecorry.sol.math.Vector3
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class PlaneMotionSolverTest {

    @Test
    fun findsCameraMotionFromGroundFeatures() {
        val scene = SyntheticScene(240, 320, 56.0)
        val solver = PlaneMotionSolver(scene.projector, 120f, 160f)
        val random = Random(5)
        val errors = mutableListOf<Float>()
        repeat(20) {
            val height = 1.2f + random.nextFloat() * 0.6f
            val rotA = floatArrayOf(random.nextFloat() * 6f, 0.4f + random.nextFloat() * 0.4f, 0f)
            val rotB = floatArrayOf(rotA[0] + 0.05f, rotA[1] - 0.03f, 0.02f)
            // The IMU reports frame b's rotation a little wrong
            val reported = floatArrayOf(rotB[0] + 0.005f, rotB[1] - 0.004f, rotB[2] + 0.003f)
            val camA = Vector3(0f, 0f, height)
            val move = Vector3(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f, (random.nextFloat() - 0.5f) * 0.1f) * 0.5f
            val camB = camA + move

            val observations = mutableListOf<PlaneMotionSolver.Observation>()
            var id = 0L
            for (y in 20 until 300 step 25) for (x in 20 until 220 step 25) {
                val ray = scene.projector.ray(x.toFloat(), y.toFloat(), rotA)
                if (ray.z > -0.1f) continue
                // 20% of features are on things that aren't the ground
                val onGround = random.nextFloat() > 0.2f
                val depth = if (onGround) -height / ray.z else -height / ray.z * 0.6f
                val point = camA + ray * depth
                val toB = point - camB
                val (px, py) = scene.projector.pixel(toB * (1f / toB.magnitude()), rotB) ?: continue
                observations.add(
                    PlaneMotionSolver.Observation(
                        id++, ray,
                        px + (random.nextFloat() - 0.5f) * 0.6f,
                        py + (random.nextFloat() - 0.5f) * 0.6f
                    )
                )
            }
            val solution = solver.solve(observations, reported, Vector3.zero) ?: return@repeat
            val truth = move * (1f / height)
            errors.add((solution.position - truth).magnitude() / truth.magnitude())
        }
        errors.sort()
        assertTrue(errors.size >= 18)
        assertTrue(errors[errors.size / 2] < 0.03f)
    }
}
