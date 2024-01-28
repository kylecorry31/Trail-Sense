package com.kylecorry.trail_sense.tools.paths.domain.waypointcolors

import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

interface IPointColoringStrategy {

    @ColorInt
    fun getColor(point: PathPoint): Int?

}