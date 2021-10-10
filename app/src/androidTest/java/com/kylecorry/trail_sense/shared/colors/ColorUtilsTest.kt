package com.kylecorry.trail_sense.shared.colors

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorUtilsTest {
    @Test
    fun nrbr() {
        assertEquals(0f, ColorUtils.nrbr(Color.BLACK), 0.0001f)
        assertEquals(-1f, ColorUtils.nrbr(Color.BLUE), 0.0001f)
        assertEquals(1f, ColorUtils.nrbr(Color.RED), 0.0001f)
        assertEquals(0.3333333f, ColorUtils.nrbr(Color.rgb(100, 0, 50)), 0.0001f)
        assertEquals(-0.3333333f, ColorUtils.nrbr(Color.rgb(50, 0, 100)), 0.0001f)
    }

    @Test
    fun average() {
        assertEquals(0f, ColorUtils.average(Color.BLACK), 0.0001f)
        assertEquals(255f, ColorUtils.average(Color.WHITE), 0.0001f)
        assertEquals(100f, ColorUtils.average(Color.rgb(200, 100, 0)), 0.0001f)
        assertEquals(50f, ColorUtils.average(Color.rgb(50, 50, 50)), 0.0001f)
    }

    @Test
    fun saturation() {
        assertEquals(0f, ColorUtils.saturation(Color.BLACK), 0.0001f)
        assertEquals(0f, ColorUtils.saturation(Color.WHITE), 0.0001f)
        assertEquals(1f, ColorUtils.saturation(Color.GREEN), 0.0001f)
        assertEquals(1f, ColorUtils.saturation(Color.BLUE), 0.0001f)
        assertEquals(1f, ColorUtils.saturation(Color.RED), 0.0001f)
        assertEquals(1f, ColorUtils.saturation(Color.rgb(255, 0, 127)), 0.0001f)
        assertEquals(0.95f, ColorUtils.saturation(Color.rgb(200, 10, 127)), 0.0001f)
        assertEquals(0.5f, ColorUtils.saturation(Color.rgb(200, 100, 127)), 0.0001f)
    }
}