package com.kylecorry.trail_sense.navigation.paths.domain

import androidx.annotation.ColorInt

data class PathStyle(
    val line: LineStyle,
    val point: PathPointColoringStyle,
    @ColorInt val color: Int,
    val visible: Boolean
)