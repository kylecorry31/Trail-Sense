package com.kylecorry.trail_sense.tools.paths.domain.pathsort

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.grouping.sort.GroupSort
import com.kylecorry.trail_sense.tools.paths.domain.IPath
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.pathsort.mappers.PathDistanceMapper

class ClosestPathSortStrategy(
    location: Coordinate,
    pathService: IPathService
) : IPathSortStrategy {

    private val sort = GroupSort(
        PathDistanceMapper(pathService.loader()) { location }
    )

    override suspend fun sort(paths: List<IPath>): List<IPath> {
        return sort.sort(paths)
    }
}