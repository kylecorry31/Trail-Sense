package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.resizeToFit
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.colors.ColorUtils

class PercentOfMaxStarFinder(private val percent: Float = 0.8f) : StarFinder {
    override fun findStars(image: Bitmap): List<PixelCoordinate> {
        val resized = image.resizeToFit(600, 600)

        try {
            var brightestValue = 0f
            for (x in 0 until resized.width) {
                for (y in 0 until resized.height) {
                    val pixel = resized.getPixel(x, y)
                    val brightness = ColorUtils.average(pixel)
                    if (brightness > brightestValue) {
                        brightestValue = brightness
                    }
                }
            }

            resized.recycle()

            val simpleFinder = SimpleStarFinder(brightestValue * percent)
            return simpleFinder.findStars(image)
        } finally {
            resized.recycle()
        }
    }
}