package com.kylecorry.trail_sense.tools.navigation.domain.paths

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

internal interface RoutingStrategy {
    fun buildRoute(points: List<PathPoint>, location: Coordinate): List<PathPoint>
}
