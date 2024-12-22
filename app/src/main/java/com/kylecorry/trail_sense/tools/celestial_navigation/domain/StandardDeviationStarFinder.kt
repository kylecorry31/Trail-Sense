package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.BitmapUtils.average
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeToFit
import com.kylecorry.andromeda.bitmaps.BitmapUtils.standardDeviation
import com.kylecorry.andromeda.core.units.PixelCoordinate

class StandardDeviationStarFinder(
    private val sigma: Float = 4f,
    private val minBrightness: Float = 40f,
    private val maxBrightness: Float = 240f,
    private val imageSize: Int = 600
) : StarFinder {
    override fun findStars(image: Bitmap): List<PixelCoordinate> {
        val resized = image.resizeToFit(imageSize, imageSize)

        try {
            val mean = resized.average()
            val stdDev = resized.standardDeviation(average = mean)
            resized.recycle()

            val simpleFinder =
                SimpleStarFinder(
                    (mean + sigma * stdDev).coerceIn(
                        minBrightness,
                        maxBrightness
                    ),
                    imageSize = imageSize
                )
            return simpleFinder.findStars(image)
        } finally {
            resized.recycle()
        }
    }
}