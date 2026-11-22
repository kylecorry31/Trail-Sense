package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import com.kylecorry.sol.math.Vector3
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class FeatureTrackerTest {

    @Test
    fun tracksGroundFeaturesBetweenFrames() {
        val scene = SyntheticScene(240, 320, 56.0)
        val rotA = floatArrayOf(0.3f, 0.6f, 0f)
        val rotB = floatArrayOf(0.32f, 0.59f, 0.01f)
        val camA = Vector3(0f, 0f, 1.5f)
        val camB = Vector3(0.03f, 0.08f, 1.52f)
        val a = scene.render(camA, rotA)
        val b = scene.render(camB, rotB)

        val tracker = FeatureTracker()
        val first = tracker.track(a)
        val second = tracker.track(b).associateBy { it.id }

        val errors = first.mapNotNull { f ->
            val moved = second[f.id] ?: return@mapNotNull null
            val ray = scene.projector.ray(f.x, f.y, rotA)
            if (ray.z >= -0.01f) return@mapNotNull null
            val ground = camA + ray * (-camA.z / ray.z)
            val toB = ground - camB
            val (px, py) = scene.projector.pixel(toB * (1f / toB.magnitude()), rotB) ?: return@mapNotNull null
            abs(px - moved.x) + abs(py - moved.y)
        }.sorted()
        val median = errors[errors.size / 2]
        assertTrue(first.size > 40)
        assertTrue(errors.size > first.size * 0.6)
        assertTrue(median < 0.3f)
    }
}
