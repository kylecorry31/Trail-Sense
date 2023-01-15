package com.kylecorry.trail_sense.tools.maps.infrastructure

import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.grouping.filter.GroupFilter
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import com.kylecorry.trail_sense.shared.grouping.persistence.ISearchableGroupLoader
import com.kylecorry.trail_sense.tools.maps.domain.IMap

class MapGroupLoader(private val loader: IGroupLoader<IMap>) : ISearchableGroupLoader<IMap> {

    private val filter = GroupFilter(loader)

    override suspend fun getGroup(id: Long): IMap? {
        return loader.getGroup(id)
    }

    override suspend fun load(search: String?, group: Long?): List<IMap> {
        return if (search.isNullOrBlank()) {
            getPathsByGroup(group)
        } else {
            getPathsBySearch(search, group)
        }
    }

    private suspend fun getPathsBySearch(search: String, groupFilter: Long?) = onIO {
        filter.filter(groupFilter) {
            it.name.contains(search, ignoreCase = true)
        }
    }

    private suspend fun getPathsByGroup(group: Long?) = onIO {
        loader.getChildren(group, 1)
    }
}