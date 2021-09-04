package com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors

import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.shared.paths.PathPoint

interface IPointColoringStrategy {

    @ColorInt
    fun getColor(point: PathPoint): Int?

}