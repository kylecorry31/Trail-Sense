package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.BitmapUtils.add
import com.kylecorry.andromeda.bitmaps.BitmapUtils.blur
import com.kylecorry.andromeda.bitmaps.BitmapUtils.minMax
import com.kylecorry.andromeda.core.units.PixelCoordinate

class DifferenceOfGaussiansStarFinder(
    private val percent: Float = 0.3f,
    private val firstBlur: Int = 1,
    private val secondBlur: Int = 4
) : StarFinder {
    override fun findStars(image: Bitmap): List<PixelCoordinate> {
        val first = image.blur(firstBlur)
        val second = image.blur(secondBlur)
        return try {
            first.add(second, 1f, -1f, absolute = true, inPlace = true)
            SimpleStarFinder(percent * first.minMax().end).findStars(first)
        } finally {
            first.recycle()
            second.recycle()
        }
    }
}
