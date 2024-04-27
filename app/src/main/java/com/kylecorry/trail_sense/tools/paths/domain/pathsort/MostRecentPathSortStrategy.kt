package com.kylecorry.trail_sense.tools.paths.domain.pathsort

import com.kylecorry.trail_sense.shared.grouping.sort.GroupSort
import com.kylecorry.trail_sense.tools.paths.domain.IPath
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.pathsort.mappers.PathIdMapper

class MostRecentPathSortStrategy(
    pathService: IPathService
) : IPathSortStrategy {
    private val sort = GroupSort(
        PathIdMapper(pathService.loader()),
        ascending = false
    )

    override suspend fun sort(paths: List<IPath>): List<IPath> {
        return sort.sort(paths)
    }
}