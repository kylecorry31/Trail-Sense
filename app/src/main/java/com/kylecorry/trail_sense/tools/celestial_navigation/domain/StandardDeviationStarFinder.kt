package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.BitmapUtils.average
import com.kylecorry.andromeda.bitmaps.BitmapUtils.standardDeviation
import com.kylecorry.andromeda.core.units.PixelCoordinate

class StandardDeviationStarFinder(
    private val sigma: Float = 4f,
    private val minBrightness: Float = 40f,
    private val maxBrightness: Float = 240f
) : StarFinder {
    override fun findStars(image: Bitmap): List<PixelCoordinate> {
        val mean = image.average()
        val stdDev = image.standardDeviation(average = mean)

        val threshold = (mean + sigma * stdDev).coerceIn(
            minBrightness,
            maxBrightness
        )

        return SimpleStarFinder(threshold).findStars(image)
    }
}