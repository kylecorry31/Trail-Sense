package com.kylecorry.trail_sense.navigation.paths.domain.point_finder

import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint

class NearestPathLineNavigator :
    IPathPointNavigator {
    override suspend fun getNextPoint(path: List<PathPoint>, location: Coordinate): PathPoint? {
        val line = NearestPathLineCalculator().calculate(location, path) ?: return null
        val nearest =
            Geology.getNearestPoint(location, line.first.coordinate, line.second.coordinate)
        return PathPoint(0, path.first().pathId, nearest)
    }
}