package com.kylecorry.trail_sense.tools.map.infrastructure

import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.trail_sense.shared.grouping.filter.GroupFilter
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import com.kylecorry.trail_sense.shared.grouping.persistence.ISearchableGroupLoader
import com.kylecorry.trail_sense.tools.map.domain.IOfflineMapFile

class OfflineMapFileGroupLoader(
    private val loader: IGroupLoader<IOfflineMapFile>
) : ISearchableGroupLoader<IOfflineMapFile> {

    private val filter = GroupFilter(loader)

    override suspend fun getGroup(id: Long): IOfflineMapFile? {
        return loader.getGroup(id)
    }

    override suspend fun load(search: String?, group: Long?): List<IOfflineMapFile> {
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
