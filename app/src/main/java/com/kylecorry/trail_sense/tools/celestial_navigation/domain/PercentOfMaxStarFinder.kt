package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.BitmapUtils.minMax
import com.kylecorry.andromeda.core.units.PixelCoordinate

class PercentOfMaxStarFinder(private val percent: Float = 0.8f) :
    StarFinder {
    override fun findStars(image: Bitmap): List<PixelCoordinate> {
        val range = image.minMax()
        return SimpleStarFinder(range.end * percent).findStars(image)
    }
}