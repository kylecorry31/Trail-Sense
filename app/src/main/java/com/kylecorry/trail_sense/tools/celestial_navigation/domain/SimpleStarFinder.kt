package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import android.util.Range
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.camera.GrayscalePointFinder

class SimpleStarFinder(
    private val threshold: Float = 200f,
    private val minRadius: Float = 0.5f
) : StarFinder {
    override fun findStars(image: Bitmap): List<PixelCoordinate> {
        val pointFinder = GrayscalePointFinder(threshold, minRadius, Range(0.5f, 2f))
        return pointFinder.getPoints(image).map {
            PixelCoordinate(it.center.x, it.center.y)
        }.filter { it.x != 0f && it.y != 0f }
    }
}