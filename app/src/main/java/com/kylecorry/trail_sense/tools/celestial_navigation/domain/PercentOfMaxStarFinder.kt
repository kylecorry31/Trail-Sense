package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.BitmapUtils.minMax
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeToFit
import com.kylecorry.andromeda.core.units.PixelCoordinate

class PercentOfMaxStarFinder(private val percent: Float = 0.8f) : StarFinder {
    override fun findStars(image: Bitmap): List<PixelCoordinate> {
        val resized = image.resizeToFit(1000, 1000)

        try {
            val range = resized.minMax()
            resized.recycle()
            val simpleFinder = SimpleStarFinder(range.end * percent, imageSize = 1000)
            return simpleFinder.findStars(image)
        } finally {
            resized.recycle()
        }
    }
}