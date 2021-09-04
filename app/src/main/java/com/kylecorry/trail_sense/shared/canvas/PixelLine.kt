package com.kylecorry.trail_sense.shared.canvas

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.core.units.PixelCoordinate

data class PixelLine(
    val start: PixelCoordinate,
    val end: PixelCoordinate,
    @ColorInt val color: Int,
    val alpha: Int = 255,
    val style: PixelLineStyle
)

