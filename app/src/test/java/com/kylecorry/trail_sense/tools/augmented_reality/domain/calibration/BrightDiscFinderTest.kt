package com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.random.Random

class BrightDiscFinderTest {

    private val w = 1080
    private val h = 1920

    private fun image(background: Int, draw: (x: Int, y: Int) -> Double): IntArray {
        val random = Random(1)
        return IntArray(w * h) {
            val x = it % w
            val y = it / w
            val v = (background + draw(x, y) + random.nextDouble(-3.0, 3.0)).toInt().coerceIn(0, 255)
            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
    }

    @Test
    fun findsSunWithGlare() {
        // Sun at (600.3, 700.7), ~9 px radius disc with a wide glow, on a bright sky
        val pixels = image(170) { x, y ->
            val d = hypot(x + 0.5 - 600.3, y + 0.5 - 700.7)
            if (d < 9) 255.0 else 120 * exp(-(d - 9) / 25)
        }
        val result = BrightDiscFinder(minPeak = 245, minContrast = 30)
            .find(pixels, w, h, 640.0, 680.0, 200.0, 60.0)
        assertNotNull(result)
        assertEquals(600.3, result!!.x, 1.0)
        assertEquals(700.7, result.y, 1.0)
    }

    @Test
    fun findsCrescentMoon() {
        // A crescent is offset from the disc center, so this shows the size of that bias
        val cx = 400.0
        val cy = 900.0
        val r = 6.0
        val pixels = image(10) { x, y ->
            val inside = hypot(x + 0.5 - cx, y + 0.5 - cy) < r
            val shadow = hypot(x + 0.5 - (cx - 4), y + 0.5 - cy) < r
            if (inside && !shadow) 230.0 else 0.0
        }
        val result = BrightDiscFinder(minPeak = 150, minContrast = 80, maxRoundnessRatio = 3.0, minFill = 0.3)
            .find(pixels, w, h, 420.0, 880.0, 200.0, 30.0)
        assertNotNull(result)
        println("Crescent offset from center: %.1f px (radius %.0f px)".format(hypot(result!!.x - cx, result.y - cy), r))
    }

    @Test
    fun rejectsElongatedLight() {
        val pixels = image(20) { x, y ->
            if (x in 300..360 && y in 500..508) 255.0 else 0.0
        }
        assertNull(BrightDiscFinder().find(pixels, w, h, 330.0, 504.0, 200.0, 60.0))
    }

    @Test
    fun ignoresDimFrame() {
        val pixels = image(60) { _, _ -> 0.0 }
        assertNull(BrightDiscFinder().find(pixels, w, h, 500.0, 500.0, 200.0, 60.0))
    }
}
