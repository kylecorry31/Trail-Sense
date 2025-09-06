package com.kylecorry.trail_sense.shared.dem.colors

import androidx.annotation.ColorInt

class SingleColorElevationColorMap(@ColorInt private val color: Int) : ElevationColorMap {
    override fun getElevationColor(meters: Float): Int {
        return color
    }

    override fun getColor(percent: Float): Int {
        return color
    }
}