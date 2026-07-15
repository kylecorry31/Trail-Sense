package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class MapCornerDetectorTest {

    @Test
    fun detectsAndScalesMapCorners() {
        val bitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)
        Canvas(bitmap).drawRect(
            60f,
            45f,
            340f,
            255f,
            Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
        )

        val bounds = requireNotNull(MapCornerDetector(maxDimension = 150).detect(bitmap))

        assertCoordinate(60f, 45f, bounds.topLeft.x, bounds.topLeft.y)
        assertCoordinate(340f, 45f, bounds.topRight.x, bounds.topRight.y)
        assertCoordinate(60f, 255f, bounds.bottomLeft.x, bounds.bottomLeft.y)
        assertCoordinate(340f, 255f, bounds.bottomRight.x, bounds.bottomRight.y)
    }

    @Test
    fun rejectsUniformImage() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)

        assertNull(MapCornerDetector().detect(bitmap))
    }

    @Test
    fun detectsWarpedNoisyMap() {
        val width = 400
        val height = 300
        val random = Random(1)
        val pixels = IntArray(width * height) {
            val brightness = 100 + random.nextInt(-10, 11)
            Color.rgb(brightness, brightness, brightness)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        val boundary = Path().apply {
            moveTo(70f, 50f)
            lineTo(335f, 75f)
            lineTo(310f, 255f)
            lineTo(90f, 235f)
            close()
        }
        Canvas(bitmap).drawPath(
            boundary,
            Paint().apply {
                color = Color.rgb(180, 180, 180)
                style = Paint.Style.FILL
            }
        )

        val bounds = requireNotNull(MapCornerDetector(maxDimension = 100).detect(bitmap))

        assertCoordinate(70f, 50f, bounds.topLeft.x, bounds.topLeft.y)
        assertCoordinate(335f, 75f, bounds.topRight.x, bounds.topRight.y)
        assertCoordinate(90f, 235f, bounds.bottomLeft.x, bounds.bottomLeft.y)
        assertCoordinate(310f, 255f, bounds.bottomRight.x, bounds.bottomRight.y)
    }

    private fun assertCoordinate(expectedX: Float, expectedY: Float, actualX: Float, actualY: Float) {
        assertEquals(expectedX, actualX, 5f)
        assertEquals(expectedY, actualY, 5f)
    }
}
