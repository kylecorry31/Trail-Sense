package com.kylecorry.trail_sense.navigation.paths.domain.point_finder

import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.science.geology.IGeologyService
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint

class NearestPathLineNavigator(private val geology: IGeologyService = GeologyService()) :
    IPathPointNavigator {


    override suspend fun getNextPoint(path: List<PathPoint>, location: Coordinate): PathPoint? {
        val lines = path.zipWithNext()
        val nearest = lines
            .map { geology.getNearestPoint(location, it.first.coordinate, it.second.coordinate) }
            .minByOrNull {
                it.distanceTo(location)
            } ?: return null

        return PathPoint(0, path.first().pathId, nearest)
    }
}