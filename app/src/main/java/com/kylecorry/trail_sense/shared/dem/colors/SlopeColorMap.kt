package com.kylecorry.trail_sense.shared.dem.colors

import androidx.annotation.ColorInt

interface SlopeColorMap {
    @ColorInt
    fun getSlopeColor(degrees: Float): Int
}
