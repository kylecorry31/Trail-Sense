package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import com.kylecorry.sol.math.Vector3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Finds how the camera moved since a keyframe from features on the nearby ground. The ground is a
 * plane (possibly sloped) one camera height below the keyframe, so a feature's keyframe ray fixes
 * its 3D point. Its pixel in the current frame then depends only on the camera's movement, the
 * slope, and the error in the IMU's rotation. Features off the plane are rejected as outliers.
 */
class PlaneMotionSolver(
    private val projector: CameraProjector,
    private val centerX: Float,
    private val centerY: Float
) {

    class Observation(val id: Long, val keyRay: Vector3, val x: Float, val y: Float)

    /**
     * @param position the camera's movement since the keyframe, in keyframe camera heights
     * @param correction yaw, pitch, and roll corrections to the frame's rotation in radians
     * @param tilt the ground's slope east and north (rise over run)
     */
    class Solution(
        val position: Vector3,
        val correction: FloatArray,
        val tilt: FloatArray,
        val inliers: Set<Long>
    )

    fun solve(
        observations: List<Observation>,
        rotation: FloatArray,
        initial: Vector3,
        initialCorrection: FloatArray = FloatArray(3),
        initialTilt: FloatArray = FloatArray(2)
    ): Solution? {
        val ground = observations.filter { it.keyRay.z < -MIN_DEPRESSION }
        if (ground.size < MIN_POINTS) return null
        val forward = projector.ray(centerX, centerY, rotation)

        var params = floatArrayOf(
            initial.x, initial.y, initial.z,
            initialCorrection[0], initialCorrection[1], initialCorrection[2],
            initialTilt[0], initialTilt[1]
        )
        var active = ground
        repeat(2) { pass ->
            params = gaussNewton(active, rotation, forward, params, if (pass == 0) HUBER_WIDE else HUBER)
            val residuals = active.map { residual(it, rotation, forward, params) }
            active = active.filterIndexed { i, _ -> residuals[i]?.let { r -> r < OUTLIER_PIXELS } == true }
            if (active.size < MIN_POINTS) return null
        }
        return Solution(
            Vector3(params[0], params[1], params[2]),
            floatArrayOf(params[3], params[4], params[5]),
            floatArrayOf(params[6], params[7]),
            active.map { it.id }.toSet()
        )
    }

    private fun gaussNewton(
        observations: List<Observation>,
        rotation: FloatArray,
        forward: Vector3,
        start: FloatArray,
        huber: Float
    ): FloatArray {
        var p = start.copyOf()
        repeat(ITERATIONS) {
            val jtj = Array(PARAMS) { DoubleArray(PARAMS) }
            val jtr = DoubleArray(PARAMS)
            for (o in observations) {
                val l = linearize(o, rotation, forward, p) ?: continue
                val norm = sqrt(l.rx * l.rx + l.ry * l.ry)
                val w = if (norm <= huber) 1.0 else huber / norm
                for (i in 0 until PARAMS) {
                    jtr[i] += w * (l.jx[i] * l.rx + l.jy[i] * l.ry)
                    for (j in 0 until PARAMS) jtj[i][j] += w * (l.jx[i] * l.jx[j] + l.jy[i] * l.jy[j])
                }
            }
            // The IMU rotation is close and most ground is near level, so keep those small unless
            // the image insists
            for (i in 3 until PARAMS) {
                val sigma = if (i < 6) ROTATION_PRIOR else TILT_PRIOR
                val prior = 1.0 / (sigma * sigma)
                jtj[i][i] += prior
                jtr[i] += prior * p[i]
            }
            for (i in 0 until PARAMS) jtj[i][i] += 1e-6 + jtj[i][i] * 1e-3
            val delta = solveLinear(jtj, jtr) ?: return p
            p = FloatArray(PARAMS) { (p[it] - delta[it]).toFloat() }
            if (delta.maxOf { abs(it) } < 1e-6) return p
        }
        return p
    }

    private class Linearization(val rx: Double, val ry: Double, val jx: DoubleArray, val jy: DoubleArray)

    // The pixel residual and its numeric Jacobian
    private fun linearize(o: Observation, rotation: FloatArray, forward: Vector3, p: FloatArray): Linearization? {
        val base = predict(o, rotation, forward, p) ?: return null
        val jx = DoubleArray(PARAMS)
        val jy = DoubleArray(PARAMS)
        for (k in 0 until PARAMS) {
            val q = p.copyOf()
            q[k] += STEPS[k]
            val moved = predict(o, rotation, forward, q) ?: return null
            jx[k] = (moved.first - base.first) / STEPS[k].toDouble()
            jy[k] = (moved.second - base.second) / STEPS[k].toDouble()
        }
        return Linearization((base.first - o.x).toDouble(), (base.second - o.y).toDouble(), jx, jy)
    }

    private fun residual(o: Observation, rotation: FloatArray, forward: Vector3, p: FloatArray): Float? {
        val (x, y) = predict(o, rotation, forward, p) ?: return null
        val dx = x - o.x
        val dy = y - o.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun predict(o: Observation, rotation: FloatArray, forward: Vector3, p: FloatArray): Pair<Float, Float>? {
        // The ground point, on the plane z = tiltX * x + tiltY * y - 1 below the keyframe
        val denom = o.keyRay.z - p[6] * o.keyRay.x - p[7] * o.keyRay.y
        if (denom > -MIN_DEPRESSION / 2) return null
        val point = o.keyRay * (-1f / denom)
        var v = point - Vector3(p[0], p[1], p[2])
        val m = v.magnitude()
        if (m < 1e-4f) return null
        v *= 1f / m
        v = rotate(v, UP, p[3])
        val pitchAxis = normalize(Vector3(forward.y, -forward.x, 0f))
        v = rotate(v, pitchAxis, p[4])
        v = rotate(v, forward, p[5])
        return projector.pixel(v, rotation)
    }

    /** The world ray through a pixel, after undoing a correction this solver found */
    fun correctedRay(x: Float, y: Float, rotation: FloatArray, correction: FloatArray): Vector3 {
        val forward = projector.ray(centerX, centerY, rotation)
        val pitchAxis = normalize(Vector3(forward.y, -forward.x, 0f))
        var v = projector.ray(x, y, rotation)
        v = rotate(v, forward, -correction[2])
        v = rotate(v, pitchAxis, -correction[1])
        v = rotate(v, UP, -correction[0])
        return v
    }

    private fun rotate(v: Vector3, axis: Vector3, angle: Float): Vector3 {
        if (angle == 0f) return v
        val c = cos(angle)
        val s = sin(angle)
        return v * c + axis.cross(v) * s + axis * (axis.dot(v) * (1 - c))
    }

    private fun normalize(v: Vector3): Vector3 {
        val m = v.magnitude()
        return if (m < 1e-6f) Vector3(1f, 0f, 0f) else v * (1f / m)
    }

    private fun solveLinear(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val n = b.size
        val m = Array(n) { i -> DoubleArray(n + 1) { j -> if (j < n) a[i][j] else b[i] } }
        for (col in 0 until n) {
            val pivot = (col until n).maxBy { abs(m[it][col]) }
            if (abs(m[pivot][col]) < 1e-12) return null
            val tmp = m[col]; m[col] = m[pivot]; m[pivot] = tmp
            for (row in 0 until n) {
                if (row == col) continue
                val f = m[row][col] / m[col][col]
                for (k in col..n) m[row][k] -= f * m[col][k]
            }
        }
        return DoubleArray(n) { m[it][n] / m[it][it] }
    }

    private companion object {
        val UP = Vector3(0f, 0f, 1f)
        const val PARAMS = 8
        val STEPS = floatArrayOf(1e-3f, 1e-3f, 1e-3f, 1e-4f, 1e-4f, 1e-4f, 1e-3f, 1e-3f)
        const val MIN_DEPRESSION = 0.1f // rays at least ~6 degrees below the horizon
        const val MIN_POINTS = 8
        const val ITERATIONS = 8
        const val HUBER_WIDE = 4f
        const val HUBER = 1.5f
        const val OUTLIER_PIXELS = 2.5f
        const val ROTATION_PRIOR = 0.02f // ~1 degree
        const val TILT_PRIOR = 0.2f
    }
}
