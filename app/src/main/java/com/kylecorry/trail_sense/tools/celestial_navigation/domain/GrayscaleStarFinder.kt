package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.BitmapUtils.gray
import com.kylecorry.andromeda.core.units.PixelCoordinate

class GrayscaleStarFinder(private val finder: StarFinder, private val inPlace: Boolean) :
    StarFinder {
    override fun findStars(image: Bitmap): List<PixelCoordinate> {
        val gray = image.gray(average = true, inPlace = inPlace)
        try {
            return finder.findStars(gray)
        } finally {
            if (gray != image) {
                gray.recycle()
            }
        }
    }
}