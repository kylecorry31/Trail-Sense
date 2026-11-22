package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import com.kylecorry.sol.math.Vector3
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Finds the camera's height in meters. The camera tracks its position in units of that height, so
 * over the last few seconds its acceleration times the height must match the accelerometer (plus a
 * bias). Walking or waving the phone is enough; standing still gives no answer.
 *
 * Camera frames arrive some time after they're captured, so the delay is found too by keeping the
 * one that fits best.
 */
class ScaleEstimator {

    class Estimate(val height: Float, val relativeError: Float)

    private class Sample(val time: Double, val value: Vector3)

    private class Row(val start: Double, val center: Double, val end: Double, val vision: Vector3)

    private class Fit(val inverseHeight: Double, val standardError: Double, val sse: Double, val rows: Int)

    private val vision = ArrayDeque<Sample>()
    private val imu = ArrayDeque<Sample>()

    fun addVision(time: Double, position: Vector3) {
        vision.addLast(Sample(time, position))
        trim(vision, time)
    }

    fun addAcceleration(time: Double, acceleration: Vector3) {
        imu.addLast(Sample(time, acceleration))
        trim(imu, time)
    }

    fun reset() {
        vision.clear()
        imu.clear()
    }

    fun estimate(): Estimate? {
        val rows = visionRows()
        if (rows.size < MIN_ROWS || imu.size < 10) return null
        var best: Fit? = null
        var delay = -MAX_DELAY
        while (delay <= MAX_DELAY + 1e-9) {
            val fit = fit(rows, delay)
            if (fit != null && (best == null || fit.sse / fit.rows < best.sse / best.rows)) {
                best = fit
            }
            delay += DELAY_STEP
        }
        val fit = best ?: return null
        if (fit.inverseHeight <= 0) return null
        return Estimate((1 / fit.inverseHeight).toFloat(), (fit.standardError / fit.inverseHeight).toFloat())
    }

    // Second differences a fixed time apart, whatever the frame rate, so the bob of walking
    // (about 2 steps a second) isn't averaged away
    private fun visionRows(): List<Row> {
        val samples = vision.toList()
        return samples.indices.mapNotNull { i ->
            val center = samples[i]
            val before = samples.subList(0, i).lastOrNull { center.time - it.time >= SPAN }
            val after = samples.subList(i + 1, samples.size).firstOrNull { it.time - center.time >= SPAN }
            if (before == null || after == null) return@mapNotNull null
            val d1 = center.time - before.time
            val d2 = after.time - center.time
            if (d1 > MAX_SPAN || d2 > MAX_SPAN) return@mapNotNull null
            val a = (after.value - center.value) * (1f / d2.toFloat()) - (center.value - before.value) * (1f / d1.toFloat())
            Row(before.time, center.time, after.time, a * (2f / (d1 + d2).toFloat()))
        }
    }

    // vision = imu / h + offset for each axis. The camera's acceleration is the noisy one, so it
    // goes on the left; fitting the other way biases the height low.
    private fun fit(rows: List<Row>, delay: Double): Fit? {
        val ata = Array(4) { DoubleArray(4) }
        val atb = DoubleArray(4)
        val pairs = rows.mapNotNull { row ->
            triangleAverage(row.start - delay, row.center - delay, row.end - delay)?.let { row.vision to it }
        }
        if (pairs.size < MIN_ROWS) return null
        for ((v, m) in pairs) {
            val vs = floatArrayOf(v.x, v.y, v.z)
            val ms = floatArrayOf(m.x, m.y, m.z)
            for (axis in 0..2) {
                val row = DoubleArray(4)
                row[0] = ms[axis].toDouble()
                row[axis + 1] = 1.0
                for (i in 0..3) {
                    atb[i] += row[i] * vs[axis]
                    for (j in 0..3) ata[i][j] += row[i] * row[j]
                }
            }
        }
        val inverse = invert(ata) ?: return null
        val solution = DoubleArray(4) { i -> (0..3).sumOf { inverse[i][it] * atb[it] } }
        var sse = 0.0
        for ((v, m) in pairs) {
            val vs = floatArrayOf(v.x, v.y, v.z)
            val ms = floatArrayOf(m.x, m.y, m.z)
            for (axis in 0..2) {
                val r = vs[axis] - (solution[0] * ms[axis] + solution[axis + 1])
                sse += r * r
            }
        }
        val dof = (pairs.size * 3 - 4).coerceAtLeast(1)
        return Fit(solution[0], sqrt(sse / dof * inverse[0][0]), sse, pairs.size)
    }

    // The camera's second difference equals the acceleration averaged with this triangle
    private fun triangleAverage(start: Double, center: Double, end: Double): Vector3? {
        var sum = Vector3.zero
        var weight = 0.0
        for (s in imu.filter { it.time in start..end }) {
            val w = if (s.time <= center) (s.time - start) / (center - start) else (end - s.time) / (end - center)
            sum += s.value * w.toFloat()
            weight += w
        }
        if (weight <= 0.0) return null
        return sum * (1f / weight.toFloat())
    }

    private fun trim(samples: ArrayDeque<Sample>, now: Double) {
        while (samples.isNotEmpty() && now - samples.first().time > WINDOW_SECONDS) samples.removeFirst()
    }

    private fun invert(a: Array<DoubleArray>): Array<DoubleArray>? {
        val n = a.size
        val m = Array(n) { i -> DoubleArray(2 * n) { j -> if (j < n) a[i][j] else if (j - n == i) 1.0 else 0.0 } }
        for (col in 0 until n) {
            val pivot = (col until n).maxBy { abs(m[it][col]) }
            if (abs(m[pivot][col]) < 1e-12) return null
            val tmp = m[col]; m[col] = m[pivot]; m[pivot] = tmp
            val p = m[col][col]
            for (k in 0 until 2 * n) m[col][k] /= p
            for (row in 0 until n) {
                if (row == col) continue
                val f = m[row][col]
                for (k in 0 until 2 * n) m[row][k] -= f * m[col][k]
            }
        }
        return Array(n) { i -> DoubleArray(n) { j -> m[i][j + n] } }
    }

    private companion object {
        const val WINDOW_SECONDS = 4.0
        const val SPAN = 0.12
        const val MAX_SPAN = 0.3
        const val MIN_ROWS = 12
        const val MAX_DELAY = 0.15
        const val DELAY_STEP = 0.01
    }
}
