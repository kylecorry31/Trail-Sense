package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.BitmapUtils.minMax
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeToFit
import com.kylecorry.andromeda.core.units.PixelCoordinate

class PercentOfMaxStarFinder(private val percent: Float = 0.8f, private val imageSize: Int = 600) :
    StarFinder {
    override fun findStars(image: Bitmap): List<PixelCoordinate> {
        val resized = image.resizeToFit(imageSize, imageSize)

        try {
            val range = resized.minMax()
            resized.recycle()
            val simpleFinder = SimpleStarFinder(range.end * percent, imageSize = imageSize)
            return simpleFinder.findStars(image)
        } finally {
            resized.recycle()
        }
    }
}