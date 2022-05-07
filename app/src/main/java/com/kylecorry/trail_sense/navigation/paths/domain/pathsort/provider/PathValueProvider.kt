package com.kylecorry.trail_sense.navigation.paths.domain.pathsort.provider

import com.kylecorry.trail_sense.navigation.paths.domain.IPath
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.domain.Path

class PathValueProvider<T>(
    private val pathService: IPathService,
    private val mapper: suspend (path: Path) -> T,
    private val aggregator: (values: List<T>) -> T
) {

    suspend fun get(path: IPath): T {
        if (path is Path) {
            return mapper(path)
        }

        val paths =
            pathService.getPaths(path.id, includeGroups = false, maxDepth = null).map { it as Path }

        val values = paths.map { mapper(it) }
        return aggregator(values)
    }

}