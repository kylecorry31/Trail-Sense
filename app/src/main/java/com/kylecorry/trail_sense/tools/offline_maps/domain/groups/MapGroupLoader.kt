package com.kylecorry.trail_sense.tools.offline_maps.domain.groups

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.shared.grouping.filter.GroupFilter
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import com.kylecorry.trail_sense.shared.grouping.persistence.ISearchableGroupLoader
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCatalogItem

class MapGroupLoader(private val loader: IGroupLoader<OfflineMapCatalogItem>) :
    ISearchableGroupLoader<OfflineMapCatalogItem> {

    private val filter = GroupFilter(loader)

    override suspend fun getGroup(id: Long): OfflineMapCatalogItem? {
        return loader.getGroup(id)
    }

    override suspend fun load(search: String?, group: Long?): List<OfflineMapCatalogItem> {
        return if (search.isNullOrBlank()) {
            getMapsByGroup(group)
        } else {
            getMapsBySearch(search, group)
        }
    }

    private suspend fun getMapsBySearch(search: String, groupFilter: Long?) = onIO {
        filter.filter(groupFilter) {
            it.name.contains(search, ignoreCase = true)
        }
    }

    private suspend fun getMapsByGroup(group: Long?) = onIO {
        loader.getChildren(group, 1)
    }
}
