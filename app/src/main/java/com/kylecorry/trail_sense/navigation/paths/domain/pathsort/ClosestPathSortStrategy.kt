package com.kylecorry.trail_sense.navigation.paths.domain.pathsort

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.paths.domain.Path

class ClosestPathSortStrategy(private val location: Coordinate) : IPathSortStrategy {
    override fun sort(paths: List<Path>): List<Path> {
        return paths.sortedBy { it.metadata.bounds.center.distanceTo(location) }
    }
}