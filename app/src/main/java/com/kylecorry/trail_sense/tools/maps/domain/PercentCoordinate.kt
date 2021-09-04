package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.andromeda.core.units.PixelCoordinate

data class PercentCoordinate(val x: Float, val y: Float) {
    fun toPixels(width: Float, height: Float): PixelCoordinate {
        return PixelCoordinate(x * width, y * height)
    }
}
