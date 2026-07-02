package com.kylecorry.trail_sense.tools.offline_maps.domain.sort

import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCatalogItem

interface IMapSortStrategy {
    suspend fun sort(maps: List<OfflineMapCatalogItem>): List<OfflineMapCatalogItem>
}
