package com.kylecorry.trail_sense.shared.dem.colors

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.core.ui.colormaps.ColorMap

interface ElevationColorMap : ColorMap {
    @ColorInt
    fun getElevationColor(meters: Float): Int
}