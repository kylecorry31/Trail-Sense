package com.kylecorry.trail_sense.navigation.paths.domain.pathsort

import com.kylecorry.trail_sense.navigation.paths.domain.IPath

class MostRecentPathSortStrategy : IPathSortStrategy {
    override suspend fun sort(paths: List<IPath>): List<IPath> {
        return paths.sortedByDescending { it.id }
    }
}