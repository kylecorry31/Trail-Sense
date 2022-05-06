package com.kylecorry.trail_sense.navigation.paths.domain.pathsort

import com.kylecorry.trail_sense.navigation.paths.domain.IPath
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathGroup

class NamePathSortStrategy : IPathSortStrategy {
    override suspend fun sort(paths: List<IPath>): List<IPath> {
        return paths.sortedWith(compareBy(nullsLast()) {
            if (it is Path) {
                it.name
            } else {
                (it as PathGroup).name
            }
        })
    }
}