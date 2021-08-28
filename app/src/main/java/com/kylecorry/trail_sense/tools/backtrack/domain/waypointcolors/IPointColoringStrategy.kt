package com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors

import androidx.annotation.ColorInt
import com.kylecorry.trailsensecore.domain.geo.PathPoint

interface IPointColoringStrategy {

    @ColorInt
    fun getColor(point: PathPoint): Int?

}