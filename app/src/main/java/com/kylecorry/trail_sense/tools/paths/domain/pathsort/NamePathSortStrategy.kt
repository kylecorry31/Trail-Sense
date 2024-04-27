package com.kylecorry.trail_sense.tools.paths.domain.pathsort

import com.kylecorry.trail_sense.shared.grouping.sort.NullableGroupSort
import com.kylecorry.trail_sense.tools.paths.domain.IPath
import com.kylecorry.trail_sense.tools.paths.domain.pathsort.mappers.PathNameMapper

class NamePathSortStrategy : IPathSortStrategy {
    private val sort = NullableGroupSort(
        PathNameMapper(),
        sortNullsLast = true
    )

    override suspend fun sort(paths: List<IPath>): List<IPath> {
        return sort.sort(paths)
    }
}