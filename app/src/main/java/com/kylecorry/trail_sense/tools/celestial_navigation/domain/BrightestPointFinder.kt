package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import android.graphics.Color
import com.kylecorry.andromeda.core.units.PixelCoordinate
import kotlin.math.pow

class BrightestPointFinder {
    fun find(
        image: Bitmap,
        center: PixelCoordinate,
        radiusX: Float,
        radiusY: Float
    ): PixelCoordinate? {
        return find(image.width, image.height, center, radiusX, radiusY) { x, y ->
            val color = image.getPixel(x, y)
            Color.red(color) + Color.green(color) + Color.blue(color)
        }
    }

    fun find(
        width: Int,
        height: Int,
        center: PixelCoordinate,
        radiusX: Float,
        radiusY: Float,
        getBrightness: (x: Int, y: Int) -> Int
    ): PixelCoordinate? {
        if (radiusX <= 0f || radiusY <= 0f) {
            return null
        }

        val minX = (center.x - radiusX).toInt().coerceAtLeast(0)
        val maxX = (center.x + radiusX).toInt().coerceAtMost(width - 1)
        val minY = (center.y - radiusY).toInt().coerceAtLeast(0)
        val maxY = (center.y + radiusY).toInt().coerceAtMost(height - 1)
        if (minX > maxX || minY > maxY) {
            return null
        }

        var brightest: PixelCoordinate? = null
        var greatestBrightness = -1
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val normalizedX = (x - center.x) / radiusX
                val normalizedY = (y - center.y) / radiusY
                if (normalizedX.pow(2) + normalizedY.pow(2) > 1f) {
                    continue
                }
                val brightness = getBrightness(x, y)
                if (brightness > greatestBrightness) {
                    greatestBrightness = brightness
                    brightest = PixelCoordinate(x.toFloat(), y.toFloat())
                }
            }
        }
        return brightest
    }
}
