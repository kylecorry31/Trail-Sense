package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import com.kylecorry.sol.math.Vector3

/**
 * Tracks the camera against the ground near it and places tracked features in 3D. Units are the
 * camera's height above the ground when tracking started: the camera starts at the origin with the
 * ground near z = -1.
 *
 * Only the nearby ground is treated as a plane, to work out how the camera moved. Other features
 * are triangulated from where they were first seen, so slopes and objects land where they are.
 */
class VisualGroundOdometry(
    projector: CameraProjector,
    width: Int,
    height: Int
) {

    class Point(val id: Long, val world: Vector3?)

    /**
     * @param position the camera's position, or null if tracking failed this frame
     */
    class Output(val position: Vector3?, val points: List<Point>)

    private class Keyframe(val position: Vector3, val rays: Map<Long, Vector3>)

    private class Anchor(val ray: Vector3, val position: Vector3) {
        var estimate: Vector3? = null
    }

    private val features = FeatureTracker()
    private val solver = PlaneMotionSolver(projector, width / 2f, height / 2f)
    private var keyframe: Keyframe? = null
    private var position = Vector3.zero
    private var relative = Vector3.zero
    private var correction = FloatArray(3)
    private var tilt = FloatArray(2)
    private var failures = 0
    private val anchors = HashMap<Long, Anchor>()

    fun process(image: FeatureTracker.Image, rotation: FloatArray): Output {
        val tracked = features.track(image)
        val key = keyframe
        if (key == null) {
            startKeyframe(tracked, rotation, FloatArray(3))
            return Output(null, emptyList())
        }

        val observations = tracked.mapNotNull { f ->
            key.rays[f.id]?.let { PlaneMotionSolver.Observation(f.id, it, f.x, f.y) }
        }
        val keyHeight = key.position.z + 1f
        val solution = solver.solve(observations, rotation, relative * (1f / keyHeight), correction, tilt)
        if (solution == null) {
            failures++
            if (failures > MAX_FAILURES) {
                startKeyframe(tracked, rotation, FloatArray(3))
                relative = Vector3.zero
            }
            return Output(null, emptyList())
        }
        failures = 0
        relative = solution.position * keyHeight
        correction = solution.correction
        tilt = solution.tilt
        position = key.position + relative

        anchors.keys.retainAll(tracked.map { it.id }.toHashSet())
        val points = tracked.map { f ->
            val ray = solver.correctedRay(f.x, f.y, rotation, correction)
            val anchor = anchors.getOrPut(f.id) { Anchor(ray, position) }
            val keyRay = key.rays[f.id]
            // Close by, the ground plane is more precise than triangulation
            val world = if (f.id in solution.inliers && keyRay != null && isNear(keyRay)) {
                onPlane(key.position, keyRay, keyHeight)
            } else {
                stable(anchor, triangulate(anchor.position, anchor.ray, position, ray))
            }
            Point(f.id, world)
        }

        val keyTracked = observations.size.toFloat() / key.rays.size.coerceAtLeast(1)
        if (relative.magnitude() > KEYFRAME_DISTANCE * keyHeight ||
            solution.inliers.size < MIN_INLIERS || keyTracked < MIN_KEYFRAME_FRACTION
        ) {
            startKeyframe(tracked, rotation, correction)
            relative = Vector3.zero
            correction = FloatArray(3)
        }
        return Output(position, points)
    }

    fun reset() {
        features.reset()
        anchors.clear()
        keyframe = null
        position = Vector3.zero
        relative = Vector3.zero
        correction = FloatArray(3)
        tilt = FloatArray(2)
        failures = 0
    }

    // A real point settles as the baseline grows, while moving shadows, swaying plants, and slipped
    // tracks keep jumping
    private fun stable(anchor: Anchor, point: Vector3?): Vector3? {
        val previous = anchor.estimate
        anchor.estimate = point
        if (point == null || previous == null) return null
        val distance = (point - anchor.position).magnitude()
        val tolerance = STABLE_TOLERANCE * maxOf(1f, distance / 2f)
        return if ((point - previous).magnitude() < tolerance) point else null
    }

    private fun isNear(keyRay: Vector3): Boolean {
        return -keyRay.z / keyRay.magnitude() > NEAR_DEPRESSION
    }

    private fun onPlane(keyPosition: Vector3, keyRay: Vector3, keyHeight: Float): Vector3? {
        val denom = keyRay.z - tilt[0] * keyRay.x - tilt[1] * keyRay.y
        if (denom >= 0f) return null
        return keyPosition + keyRay * (-keyHeight / denom)
    }

    private fun startKeyframe(tracked: List<FeatureTracker.Feature>, rotation: FloatArray, correction: FloatArray) {
        // Each keyframe inherits the last one's height error. People hold the phone at about the
        // same height, so ease back toward it instead of letting the ground drift away.
        position = Vector3(position.x, position.y, position.z * (1 - HEIGHT_RELAXATION))
        keyframe = Keyframe(
            position,
            tracked.associate { it.id to solver.correctedRay(it.x, it.y, rotation, correction) }
        )
    }

    // Midpoint of the closest approach of two rays
    private fun triangulate(c1: Vector3, d1: Vector3, c2: Vector3, d2: Vector3): Vector3? {
        val t = c2 - c1
        val b = d1.dot(d2)
        val d = d1.dot(t)
        val e = d2.dot(t)
        val denom = 1f - b * b
        if (denom < MIN_PARALLAX) return null
        val s1 = (d - b * e) / denom
        val s2 = (b * d - e) / denom
        if (s1 <= 0f || s2 <= 0f || s1 > MAX_DEPTH) return null
        return (c1 + d1 * s1 + c2 + d2 * s2) * 0.5f
    }

    private companion object {
        const val KEYFRAME_DISTANCE = 0.25f
        const val HEIGHT_RELAXATION = 0.2f
        const val MIN_INLIERS = 20
        const val MIN_KEYFRAME_FRACTION = 0.5f
        const val MAX_FAILURES = 3
        const val MIN_PARALLAX = 3.7e-3f // sin^2 of about 3.5 degrees
        const val STABLE_TOLERANCE = 0.1f
        const val NEAR_DEPRESSION = 0.55f // about 33 degrees below the horizon
        const val MAX_DEPTH = 15f
    }
}
