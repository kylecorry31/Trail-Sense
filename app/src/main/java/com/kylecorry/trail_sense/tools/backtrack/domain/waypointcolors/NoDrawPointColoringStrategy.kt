package com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors

import com.kylecorry.trail_sense.shared.paths.PathPoint

class NoDrawPointColoringStrategy : IPointColoringStrategy {
    override fun getColor(point: PathPoint): Int? {
        return null
    }
}