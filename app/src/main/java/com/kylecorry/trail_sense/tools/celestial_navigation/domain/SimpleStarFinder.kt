package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.resizeToFit
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.camera.GrayscalePointFinder

class SimpleStarFinder(private val threshold: Float = 200f) : StarFinder {
    override fun findStars(image: Bitmap): List<PixelCoordinate> {
        val resized = image.resizeToFit(400, 400)

        val xScale = resized.width.toFloat() / image.width
        val yScale = resized.height.toFloat() / image.height

        try {
            val pointFinder = GrayscalePointFinder(threshold, 4f, 4f)
            return pointFinder.getPoints(resized).map {
                PixelCoordinate(it.center.x / xScale, it.center.y / yScale)
            }
        } finally {
            resized.recycle()
        }
    }
}