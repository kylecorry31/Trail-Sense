package com.kylecorry.trail_sense.tools.backtrack.domain.waypointvalues

import com.kylecorry.trail_sense.shared.paths.PathPoint

interface IPointValueStrategy {

    fun getValue(point: PathPoint): String

}