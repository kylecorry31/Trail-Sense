package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import com.kylecorry.sol.math.Vector3
import kotlin.math.abs

/**
 * Builds a sparse point cloud from two camera frames (block matching + triangulation using the
 * IMU rotation and a known translation) and picks the dominant horizontal plane below the camera.
 */
class GroundEstimator(private val projector: Projector) {

    interface Projector {
        /** Unit ray (East-North-Up) through a frame pixel for the given device rotation */
        fun ray(x: Float, y: Float, rotation: FloatArray): Vector3

        /** Frame pixel (x, y) of an ENU ray for the given device rotation, or null if behind */
        fun pixel(ray: Vector3, rotation: FloatArray): Pair<Float, Float>?
    }

    class Frame(val gray: IntArray, val width: Int, val height: Int, val rotation: FloatArray)

    class Result(val points: List<Vector3>, val groundDown: Float?, val groundSupport: Int)

    /**
     * @param translation Camera movement from frame a to frame b in ENU meters
     * @return Points in ENU relative to the camera position at frame b, and the estimated distance
     * from the camera down to the ground
     */
    fun estimate(a: Frame, b: Frame, translation: Vector3): Result {
        val points = mutableListOf<Vector3>()
        val margin = PATCH + 1
        var y = margin
        while (y < a.height - margin) {
            var x = margin
            while (x < a.width - margin) {
                triangulate(a, b, x, y, translation)?.let { points.add(it) }
                x += STEP
            }
            y += STEP
        }
        val (ground, support) = dominantPlane(points)
        return Result(points, ground, support)
    }

    // Plane sweep: try candidate depths along the ray from frame a, project each hypothesis into
    // frame b (knowing the rotation and translation) and keep the depth whose patch matches best.
    private fun triangulate(a: Frame, b: Frame, x: Int, y: Int, t: Vector3): Vector3? {
        if (variance(a, x, y) < MIN_VARIANCE) return null

        val d1 = projector.ray(x.toFloat(), y.toFloat(), a.rotation)
        var best = Int.MAX_VALUE
        var bestPoint: Vector3? = null
        var sum = 0L
        var count = 0
        var depth = MIN_DEPTH
        while (depth <= MAX_DEPTH) {
            val inB = d1 * depth - t
            depth *= DEPTH_STEP
            val (px, py) = projector.pixel(inB * (1f / inB.magnitude()), b.rotation) ?: continue
            val bx = px.toInt()
            val by = py.toInt()
            if (bx < PATCH || by < PATCH || bx >= b.width - PATCH || by >= b.height - PATCH) continue
            val sad = sad(a, x, y, b, bx, by)
            sum += sad
            count++
            if (sad < best) {
                best = sad
                bestPoint = inB
            }
        }
        if (count < 10) return null
        // Reject ambiguous matches (the best has to stand out from the average)
        if (best > (sum / count) * MATCH_RATIO) return null
        return bestPoint
    }

    private fun dominantPlane(points: List<Vector3>): Pair<Float?, Int> {
        val bins = IntArray(BINS)
        for (p in points) {
            val down = -p.z
            if (down < MIN_GROUND || down >= MAX_GROUND) continue
            bins[((down - MIN_GROUND) / BIN).toInt().coerceAtMost(BINS - 1)]++
        }
        var bestBin = -1
        var bestCount = 0
        for (i in bins.indices) {
            val c = bins[i] + (bins.getOrNull(i - 1) ?: 0) + (bins.getOrNull(i + 1) ?: 0)
            if (c > bestCount) {
                bestCount = c
                bestBin = i
            }
        }
        if (bestBin < 0 || bestCount < MIN_SUPPORT) return null to bestCount
        val near = points.map { -it.z }.filter { abs(it - (MIN_GROUND + (bestBin + 0.5f) * BIN)) < BIN * 1.5f }
        return near.average().toFloat() to bestCount
    }

    private fun variance(f: Frame, x: Int, y: Int): Float {
        var s = 0f
        var s2 = 0f
        var n = 0
        for (dy in -PATCH..PATCH) for (dx in -PATCH..PATCH) {
            val v = f.gray[(y + dy) * f.width + x + dx].toFloat()
            s += v
            s2 += v * v
            n++
        }
        val m = s / n
        return s2 / n - m * m
    }

    private fun sad(a: Frame, ax: Int, ay: Int, b: Frame, bx: Int, by: Int): Int {
        var total = 0
        for (dy in -PATCH..PATCH) {
            val ia = (ay + dy) * a.width + ax
            val ib = (by + dy) * b.width + bx
            for (dx in -PATCH..PATCH) {
                total += abs(a.gray[ia + dx] - b.gray[ib + dx])
            }
        }
        return total
    }

    companion object {
        const val PATCH = 4
        const val DEPTH_STEP = 1.05f
        const val STEP = 8
        const val MIN_VARIANCE = 100f
        const val MATCH_RATIO = 0.5f
        const val MIN_DEPTH = 0.3f
        const val MAX_DEPTH = 12f
        const val MIN_GROUND = 0.4f
        const val MAX_GROUND = 3f
        const val BIN = 0.1f
        const val BINS = ((MAX_GROUND - MIN_GROUND) / BIN).toInt()
        const val MIN_SUPPORT = 8
    }
}
