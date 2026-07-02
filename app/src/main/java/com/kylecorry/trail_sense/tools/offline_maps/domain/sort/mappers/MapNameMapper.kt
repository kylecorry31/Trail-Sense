package com.kylecorry.trail_sense.tools.offline_maps.domain.sort.mappers

import com.kylecorry.trail_sense.shared.grouping.mapping.ISuspendMapper
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCatalogItem

class MapNameMapper : ISuspendMapper<OfflineMapCatalogItem, String> {
    override suspend fun map(item: OfflineMapCatalogItem): String {
        return item.name
    }
}
