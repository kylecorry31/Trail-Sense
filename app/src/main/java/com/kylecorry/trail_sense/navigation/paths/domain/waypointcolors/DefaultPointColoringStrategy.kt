package com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors

import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.shared.paths.PathPoint

class DefaultPointColoringStrategy(@ColorInt private val color: Int) : IPointColoringStrategy {
    override fun getColor(point: PathPoint): Int {
        return color
    }
}