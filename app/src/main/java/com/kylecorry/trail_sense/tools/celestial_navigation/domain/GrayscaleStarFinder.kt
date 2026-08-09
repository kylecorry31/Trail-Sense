package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.BitmapUtils.gray
import com.kylecorry.andromeda.core.units.PixelCoordinate

class GrayscaleStarFinder(
    private val finder: StarFinder,
    private val inPlace: Boolean
) : StarFinder {
    override fun findStars(image: Bitmap): List<PixelCoordinate> {
        val grayscale = image.gray(average = true, inPlace = inPlace)
        return try {
            finder.findStars(grayscale)
        } finally {
            if (grayscale != image) {
                grayscale.recycle()
            }
        }
    }
}
