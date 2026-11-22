package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin

class PinholeProjectorTest {

    @Test
    fun pixelsAndRaysRoundTrip() {
        val projector = PinholeProjector()
        projector.update(240, 320, 56f, 70f)
        // A rotation about a tilted axis, as a column major 4x4
        val a = 0.7f
        val k = floatArrayOf(0.3f, 0.8f, 0.52f).let { v ->
            val m = kotlin.math.sqrt(v.sumOf { (it * it).toDouble() }).toFloat()
            v.map { it / m }
        }
        val c = cos(a)
        val s = sin(a)
        val r = Array(3) { i ->
            FloatArray(3) { j ->
                val cross = when {
                    i == 0 && j == 1 -> -k[2]; i == 0 && j == 2 -> k[1]
                    i == 1 && j == 0 -> k[2]; i == 1 && j == 2 -> -k[0]
                    i == 2 && j == 0 -> -k[1]; i == 2 && j == 1 -> k[0]
                    else -> 0f
                }
                (if (i == j) c else 0f) + s * cross + (1 - c) * k[i] * k[j]
            }
        }
        val matrix = FloatArray(16)
        for (i in 0..2) for (j in 0..2) matrix[j * 4 + i] = r[i][j]
        matrix[15] = 1f

        for ((x, y) in listOf(120f to 160f, 10f to 20f, 230f to 300f, 60f to 250f)) {
            val ray = projector.ray(x, y, matrix)
            val (px, py) = projector.pixel(ray, matrix)!!
            assertEquals(x, px, 1e-3f)
            assertEquals(y, py, 1e-3f)
        }
    }
}
