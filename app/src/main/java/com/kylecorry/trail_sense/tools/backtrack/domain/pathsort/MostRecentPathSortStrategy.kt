package com.kylecorry.trail_sense.tools.backtrack.domain.pathsort

import com.kylecorry.trail_sense.shared.paths.Path

class MostRecentPathSortStrategy : IPathSortStrategy {
    override fun sort(paths: List<Path>): List<Path> {
        return paths.sortedByDescending { it.id }
    }
}