package com.kylecorry.trail_sense.navigation.paths.domain.pathsort

import com.kylecorry.trail_sense.navigation.paths.domain.Path

class MostRecentPathSortStrategy : IPathSortStrategy {
    override fun sort(paths: List<Path>): List<Path> {
        return paths.sortedByDescending { it.id }
    }
}