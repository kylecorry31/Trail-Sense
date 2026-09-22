package com.kylecorry.trail_sense.tools.paths.domain

import com.kylecorry.sol.units.Distance

class ShouldRecordBacktrackPoint(private val minimumDistance: Distance?) {

    fun isSatisfiedBy(previous: PathPoint?, point: PathPoint): Boolean {
        val minimum = minimumDistance ?: return true
        if (minimum.meters().value <= 0f) {
            return true
        }
        previous ?: return true
        return previous.coordinate.distanceTo(point.coordinate) >= minimum.meters().value
    }
}
