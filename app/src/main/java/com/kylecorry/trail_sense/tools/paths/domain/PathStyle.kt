package com.kylecorry.trail_sense.tools.paths.domain

import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.shared.colors.AppColor

data class PathStyle(
    val line: LineStyle,
    val point: PathPointColoringStyle,
    @ColorInt val color: Int,
    val visible: Boolean
){
    companion object {
        fun default(): PathStyle {
            return PathStyle(
                LineStyle.Dotted,
                PathPointColoringStyle.None,
                AppColor.Gray.color,
                true
            )
        }
    }
}