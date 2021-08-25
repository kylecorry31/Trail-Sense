package com.kylecorry.trail_sense.tools.backtrack.domain.scales

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange

interface IColorScale {

    @ColorInt
    fun getColor(@FloatRange(from = 0.0, to = 1.0) percent: Float): Int

}