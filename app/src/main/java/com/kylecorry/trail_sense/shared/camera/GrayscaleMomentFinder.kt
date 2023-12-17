package com.kylecorry.trail_sense.shared.camera

import android.graphics.Bitmap
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.colors.ColorUtils

class GrayscaleMomentFinder(private val threshold: Int, private val minPixels: Int) {

    fun getMoment(bitmap: Bitmap): PixelCoordinate? {
        var momentX = 0f
        var momentY = 0f
        var total = 0f
        var count = 0
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                val brightness = ColorUtils.average(pixel)
                if (brightness >= threshold) {
                    momentX += x * brightness
                    momentY += y * brightness
                    total += brightness
                    count++
                }
            }
        }

        if (count < minPixels || total == 0f) {
            return null
        }

        val x = momentX / total
        val y = momentY / total

        return PixelCoordinate(x, y)
    }

}