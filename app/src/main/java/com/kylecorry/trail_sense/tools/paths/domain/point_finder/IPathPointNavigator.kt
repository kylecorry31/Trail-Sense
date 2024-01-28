package com.kylecorry.trail_sense.tools.paths.domain.point_finder

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

interface IPathPointNavigator {

    suspend fun getNextPoint(path: List<PathPoint>, location: Coordinate): PathPoint?

}