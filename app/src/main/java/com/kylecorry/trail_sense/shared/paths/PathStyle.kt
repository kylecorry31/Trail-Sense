package com.kylecorry.trail_sense.shared.paths

import androidx.annotation.ColorInt

data class PathStyle(
    val style: LineStyle,
    val pointStyle: PathPointColoringStyle,
    @ColorInt val color: Int,
    val visible: Boolean
)