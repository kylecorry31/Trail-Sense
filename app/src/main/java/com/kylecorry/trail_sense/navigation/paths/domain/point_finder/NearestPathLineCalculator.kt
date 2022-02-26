package com.kylecorry.trail_sense.navigation.paths.domain.point_finder

import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.science.geology.IGeologyService
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.paths.domain.PathLine
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.navigation.paths.domain.lines

class NearestPathLineCalculator(private val geology: IGeologyService = GeologyService()) {

    fun calculate(location: Coordinate, path: List<PathPoint>): PathLine? {
        return path
            .lines()
            .minByOrNull { getDistance(location, it) }
    }

    private fun getDistance(location: Coordinate, line: PathLine): Float {
        return geology.getNearestPoint(
            location,
            line.first.coordinate,
            line.second.coordinate
        ).distanceTo(location)
    }

}