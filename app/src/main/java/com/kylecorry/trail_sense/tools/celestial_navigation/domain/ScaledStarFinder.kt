package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeToFit
import com.kylecorry.andromeda.core.units.PixelCoordinate

class ScaledStarFinder(private val finder: StarFinder, private val maxImageSize: Int) : StarFinder {
    override fun findStars(image: Bitmap): List<PixelCoordinate> {
        val resized =
            if (image.width <= maxImageSize && image.height <= maxImageSize) image else image.resizeToFit(
                maxImageSize,
                maxImageSize
            )
        val xScale = resized.width.toFloat() / image.width
        val yScale = resized.height.toFloat() / image.height

        try {
            return finder.findStars(resized).map {
                PixelCoordinate(it.x / xScale, it.y / yScale)
            }
        } finally {
            if (resized != image) {
                resized.recycle()
            }
        }

    }
}