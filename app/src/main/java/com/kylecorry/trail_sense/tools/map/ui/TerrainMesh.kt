package com.kylecorry.trail_sense.tools.map.ui

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.sinDegrees
import kotlin.math.abs

/** Terrain with perspective on distant ground and 2x relief relative to the map center. */
internal class TerrainMesh(
    val width: Float,
    val height: Float,
    elevations: FloatArray,
    resolution: Float,
    val groundHeight: Float = height * 3,
    tiltDegrees: Float = 45f,
    val groundWidth: Float = width
) {
    val vertices = FloatArray((COLUMNS + 1) * (ROWS + 1) * 2)
    private val tiltCos = cosDegrees(tiltDegrees)
    private val tiltSin = sinDegrees(tiltDegrees)
    private val focalLength = height * FOCAL_LENGTH_HEIGHT_FACTOR

    init {
        val centerElevation = elevations[(ROWS / 2) * (COLUMNS + 1) + COLUMNS / 2]
        for (row in 0..ROWS) {
            for (column in 0..COLUMNS) {
                val index = row * (COLUMNS + 1) + column
                val point = source(column, row)
                val y = point.y - height / 2
                val depth = 1f - y.coerceAtMost(0f) * tiltSin / focalLength
                vertices[index * 2] = width / 2 + (point.x - width / 2) / depth
                vertices[index * 2 + 1] = height / 2 +
                    (y * tiltCos -
                        (elevations[index] - centerElevation) / resolution * tiltSin * ELEVATION_EXAGGERATION) / depth
            }
        }
    }

    fun source(column: Int, row: Int): Vector2 =
        Vector2(width / 2 + (column.toFloat() / COLUMNS - 0.5f) * groundWidth,
            height / 2 + (row.toFloat() / ROWS - 0.5f) * groundHeight)

    /** Reverse painter order chooses the visible terrain when slopes overlap. */
    fun toMap(point: Vector2): Vector2? {
        for (row in ROWS - 1 downTo 0) {
            for (column in 0 until COLUMNS) {
                val a = row * (COLUMNS + 1) + column
                val b = a + 1
                val c = a + COLUMNS + 1
                val d = c + 1
                interpolate(point, a, c, b)?.let { return it }
                interpolate(point, b, c, d)?.let { return it }
            }
        }
        return null
    }

    private fun interpolate(point: Vector2, a: Int, b: Int, c: Int): Vector2? {
        val ax = vertices[a * 2]
        val ay = vertices[a * 2 + 1]
        val bx = vertices[b * 2]
        val by = vertices[b * 2 + 1]
        val cx = vertices[c * 2]
        val cy = vertices[c * 2 + 1]
        val denominator = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
        if (abs(denominator) < 0.0001f) return null
        val u = ((by - cy) * (point.x - cx) + (cx - bx) * (point.y - cy)) / denominator
        val v = ((cy - ay) * (point.x - cx) + (ax - cx) * (point.y - cy)) / denominator
        val w = 1 - u - v
        if (u < -0.0001f || v < -0.0001f || w < -0.0001f) return null
        val pa = source(a % (COLUMNS + 1), a / (COLUMNS + 1))
        val pb = source(b % (COLUMNS + 1), b / (COLUMNS + 1))
        val pc = source(c % (COLUMNS + 1), c / (COLUMNS + 1))
        return Vector2(pa.x * u + pb.x * v + pc.x * w, pa.y * u + pb.y * v + pc.y * w)
    }

    companion object {
        const val COLUMNS = 32
        const val ROWS = 64
        const val TILT_COS = 0.70710677f
        const val TILT_SIN = 0.70710677f
        const val ELEVATION_EXAGGERATION = 2f
        const val MAX_TILT_DEGREES = 60f
        private const val FOCAL_LENGTH_HEIGHT_FACTOR = 3f
        fun groundHeight(height: Float, elevationRange: Float, resolution: Float, tiltDegrees: Float = 45f): Float {
            // Reach beyond the top edge even when low terrain shifts the distant ground downward.
            val tiltCos = cosDegrees(tiltDegrees)
            val tiltSin = sinDegrees(tiltDegrees)
            val focalLength = height * FOCAL_LENGTH_HEIGHT_FACTOR
            val relief = elevationRange / resolution * tiltSin * ELEVATION_EXAGGERATION
            return 2 * (height * 0.6f + relief) /
                (tiltCos - height * 0.6f * tiltSin / focalLength)
        }

        fun groundWidth(width: Float, height: Float, groundHeight: Float, tiltDegrees: Float): Float {
            val farDepth = 1f + groundHeight / 2 * sinDegrees(tiltDegrees) /
                (height * FOCAL_LENGTH_HEIGHT_FACTOR)
            return width * farDepth * 1.1f
        }

        const val VERTEX_COUNT = (COLUMNS + 1) * (ROWS + 1)
    }
}
