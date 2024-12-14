package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.andromeda.core.units.PixelCoordinate

interface StarFinder {
    fun findStars(image: Bitmap): List<PixelCoordinate>
}