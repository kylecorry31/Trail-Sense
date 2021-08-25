package com.kylecorry.trail_sense.shared.scales

import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils

class ContinuousColorScale(@ColorInt private val start: Int, @ColorInt private val end: Int) :
    IColorScale {
    override fun getColor(percent: Float): Int {
        return ColorUtils.blendARGB(start, end, percent)
    }
}