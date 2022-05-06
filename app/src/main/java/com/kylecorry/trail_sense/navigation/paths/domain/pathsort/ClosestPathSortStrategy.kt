package com.kylecorry.trail_sense.navigation.paths.domain.pathsort

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.navigation.paths.domain.IPath
import com.kylecorry.trail_sense.navigation.paths.domain.Path

class ClosestPathSortStrategy(private val location: Coordinate) : IPathSortStrategy {
    override suspend fun sort(paths: List<IPath>): List<IPath> {
        return paths.sortedBy {
            if (it is Path) {
                it.metadata.bounds.center.distanceTo(location)
            } else {
                0f
            }
        }
    }
}