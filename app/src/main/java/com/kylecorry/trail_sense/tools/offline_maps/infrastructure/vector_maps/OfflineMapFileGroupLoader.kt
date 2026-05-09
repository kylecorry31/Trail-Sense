package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps

import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.trail_sense.shared.grouping.filter.GroupFilter
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import com.kylecorry.trail_sense.shared.grouping.persistence.ISearchableGroupLoader
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap

class OfflineMapFileGroupLoader(
    private val loader: IGroupLoader<IMap>
) : ISearchableGroupLoader<IMap> {

    private val filter = GroupFilter(loader)

    override suspend fun getGroup(id: Long): IMap? {
        return loader.getGroup(id)
    }

    override suspend fun load(search: String?, group: Long?): List<IMap> {
        return if (search.isNullOrBlank()) {
            getFilesByGroup(group)
        } else {
            getFilesBySearch(search, group)
        }
    }

    private suspend fun getFilesBySearch(search: String, groupFilter: Long?) = onIO {
        filter.filter(groupFilter) {
            it.name.contains(search, ignoreCase = true)
        }
    }

    private suspend fun getFilesByGroup(group: Long?) = onIO {
        loader.getChildren(group, 1)
    }
}
