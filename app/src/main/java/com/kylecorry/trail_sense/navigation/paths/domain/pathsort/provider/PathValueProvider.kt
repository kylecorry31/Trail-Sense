package com.kylecorry.trail_sense.navigation.paths.domain.pathsort.provider

import com.kylecorry.trail_sense.navigation.paths.domain.IPath
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.shared.grouping.GroupMapper

class PathValueProvider<T>(
    pathService: IPathService,
    map: suspend (path: Path) -> T,
    private val aggregator: (values: List<T>) -> T
) {

    private val mapper = GroupMapper(pathService.loader()) { map(it as Path) }

    suspend fun get(path: IPath): T {
        return aggregator(mapper.get(path))
    }

}