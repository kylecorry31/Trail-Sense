package com.kylecorry.trail_sense.navigation.paths.infrastructure

import com.kylecorry.trail_sense.navigation.paths.domain.IPath
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.grouping.ISearchableGroupLoader

class PathGroupLoader(private val pathService: IPathService) : ISearchableGroupLoader<IPath> {

    override suspend fun load(search: String?, group: Long?): List<IPath> = onIO {
        if (search.isNullOrBlank()) {
            getPathsByGroup(group)
        } else {
            getPathsBySearch(search, group)
        }
    }

    private suspend fun getPathsBySearch(search: String, groupFilter: Long?) = onIO {
        // TODO: Implement this
        emptyList<IPath>()
    }

    private suspend fun getPathsByGroup(group: Long?) = onIO {
        pathService.getPaths(group, includeGroups = true)
    }

    override suspend fun getGroup(id: Long): IPath? {
        return pathService.getGroup(id)
    }
}