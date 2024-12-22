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
        val blurred1 = image.blur(firstBlur)
        val blurred2 = image.blur(secondBlur)

        try {
            blurred1.add(blurred2, 1f, -1f, absolute = true, inPlace = true)
            val maxDiff = blurred1.minMax().end

            blurred2.recycle()

            return SimpleStarFinder(percent * maxDiff).findStars(blurred1)
        } finally {
            blurred1.recycle()
            blurred2.recycle()
        }
    }
}