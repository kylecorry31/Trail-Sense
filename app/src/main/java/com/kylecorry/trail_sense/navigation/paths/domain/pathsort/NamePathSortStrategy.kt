package com.kylecorry.trail_sense.navigation.paths.domain.pathsort

import com.kylecorry.trail_sense.navigation.paths.domain.IPath
import com.kylecorry.trail_sense.navigation.paths.domain.pathsort.mappers.PathNameMapper
import com.kylecorry.trail_sense.shared.grouping.sort.NullableGroupSort

class NamePathSortStrategy : IPathSortStrategy {
    private val sort = NullableGroupSort(
        PathNameMapper(),
        sortNullsLast = true
    )

    override suspend fun sort(paths: List<IPath>): List<IPath> {
        return sort.sort(paths)
    }
}