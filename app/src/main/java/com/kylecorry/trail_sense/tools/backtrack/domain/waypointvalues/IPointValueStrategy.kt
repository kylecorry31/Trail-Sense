package com.kylecorry.trail_sense.tools.backtrack.domain.waypointvalues

import com.kylecorry.trailsensecore.domain.geo.PathPoint

interface IPointValueStrategy {

    fun getValue(point: PathPoint): String

}