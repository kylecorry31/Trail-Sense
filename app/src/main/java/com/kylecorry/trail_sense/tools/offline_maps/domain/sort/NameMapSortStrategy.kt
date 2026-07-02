package com.kylecorry.trail_sense.tools.offline_maps.domain.sort

import com.kylecorry.trail_sense.shared.grouping.sort.GroupSort
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCatalogItem
import com.kylecorry.trail_sense.tools.offline_maps.domain.sort.mappers.MapNameMapper

class NameMapSortStrategy : IMapSortStrategy {
    private val sort = GroupSort(MapNameMapper())

    override suspend fun sort(maps: List<OfflineMapCatalogItem>): List<OfflineMapCatalogItem> {
        return sort.sort(maps)
    }
}
