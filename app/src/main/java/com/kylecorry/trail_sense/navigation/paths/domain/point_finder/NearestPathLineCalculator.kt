package com.kylecorry.trail_sense.navigation.paths.domain.point_finder

import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.paths.domain.PathLine
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.navigation.paths.domain.lines

class NearestPathLineCalculator {

    fun calculate(location: Coordinate, path: List<PathPoint>): PathLine? {
        return path
            .lines()
            .minByOrNull { getDistance(location, it) }
    }

    private fun getDistance(location: Coordinate, line: PathLine): Float {
        return Geology.getNearestPoint(
            location,
            line.first.coordinate,
            line.second.coordinate
        ).distanceTo(location)
    }

}