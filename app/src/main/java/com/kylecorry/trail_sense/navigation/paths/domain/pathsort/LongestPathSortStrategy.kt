package com.kylecorry.trail_sense.navigation.paths.domain.pathsort

import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.navigation.paths.domain.IPath
import com.kylecorry.trail_sense.navigation.paths.domain.Path

class LongestPathSortStrategy : IPathSortStrategy {
    override suspend fun sort(paths: List<IPath>): List<IPath> {
        return paths.sortedByDescending {
            if (it is Path) {
                it.metadata.distance
            } else {
                Distance.meters(0f)
            }
        }
    }
}