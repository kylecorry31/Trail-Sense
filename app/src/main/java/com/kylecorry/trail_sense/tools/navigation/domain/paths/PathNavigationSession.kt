package com.kylecorry.trail_sense.tools.navigation.domain.paths

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance

data class PathNavigationSession(
    val pathId: Long,
    val follow: PathFollow,
    val start: Coordinate,
    val progress: PathNavigationSessionProgress? = null
)

data class PathNavigationSessionProgress(
    val distanceAlong: Distance,
    val location: Coordinate
)
