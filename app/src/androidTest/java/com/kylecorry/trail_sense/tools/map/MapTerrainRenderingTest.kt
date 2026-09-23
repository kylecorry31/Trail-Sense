package com.kylecorry.trail_sense.tools.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.canvas.LineClipper
import com.kylecorry.trail_sense.shared.getBounds
import org.junit.Assert.assertEquals
import org.junit.Test

class MapTerrainRenderingTest {
    @Test
    fun contoursRenderAboveCenterInReducedTerrainTexture() {
        val bitmap = Bitmap.createBitmap(400, 1200, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.scale(0.5f, 0.5f)
            canvas.translate(0f, 800f)
            val drawer = CanvasDrawer(InstrumentationRegistry.getInstrumentation().targetContext, canvas)
            val bounds = drawer.getBounds()
            assertEquals(-800f, bounds.bottom, 0.01f)
            assertEquals(1600f, bounds.top, 0.01f)
            val segments = mutableListOf<Float>()
            LineClipper().clip(listOf(PixelCoordinate(20f, -700f), PixelCoordinate(100f, -700f)),
                bounds, segments)
            assertEquals(4, segments.size)
            val paint = Paint().apply {
                color = Color.RED
                strokeWidth = 4f
            }
            canvas.drawLines(segments.toFloatArray(), paint)
            assertEquals(Color.RED, bitmap.getPixel(25, 50))
        } finally {
            bitmap.recycle()
        }
    }
}
