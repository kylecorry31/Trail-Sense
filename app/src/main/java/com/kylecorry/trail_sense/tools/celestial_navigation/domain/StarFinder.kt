package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.trail_sense.shared.canvas.PixelCircle

interface StarFinder {
    fun findStars(image: Bitmap): List<PixelCircle>
}