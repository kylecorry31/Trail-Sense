package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.resizeToFit
import com.kylecorry.trail_sense.shared.camera.GrayscalePointFinder
import com.kylecorry.trail_sense.shared.canvas.PixelCircle

class SimpleStarFinder : StarFinder {
    override fun findStars(image: Bitmap): List<PixelCircle> {
        val resized = image.resizeToFit(400, 400)

        try {
            val pointFinder = GrayscalePointFinder(200f, 4f, 4f)
            return pointFinder.getPoints(resized)
        } finally {
            resized.recycle()
        }
    }
}