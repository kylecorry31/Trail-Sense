package com.kylecorry.trail_sense.tools.navigation.domain.paths

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

data class PathGuidance(
    val target: Coordinate,
    val remainingDistance: Distance,
    val offRouteDistance: Distance,
    val arrived: Boolean,
    val remainingRoute: List<PathPoint>,
    val remainingElevationGain: Distance,
    val remainingElevationLoss: Distance,
    val distanceAlong: Distance,
    val progress: Float
)
