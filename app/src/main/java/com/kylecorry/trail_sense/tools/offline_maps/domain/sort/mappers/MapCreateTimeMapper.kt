package com.kylecorry.trail_sense.tools.offline_maps.domain.sort.mappers

import com.kylecorry.trail_sense.shared.grouping.mapping.GroupMapper
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCatalogItem
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMap

class MapCreateTimeMapper(override val loader: IGroupLoader<OfflineMapCatalogItem>) : GroupMapper<OfflineMapCatalogItem, Long, Long>() {

    override suspend fun getValue(item: OfflineMapCatalogItem): Long {
        val time = when (item) {
            is OfflineMap -> item.createdOn
            else -> null
        }
        return time?.toEpochMilli() ?: item.id
    }

    override suspend fun aggregate(values: List<Long>): Long {
        return values.maxOrNull() ?: 0
    }

}
