package com.kylecorry.trail_sense.tools.offline_maps.domain.sort

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import com.kylecorry.trail_sense.shared.grouping.sort.GroupSort
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCatalogItem
import com.kylecorry.trail_sense.tools.offline_maps.domain.sort.mappers.MapMinimumDistanceMapper

class ClosestMapSortStrategy(
    location: Coordinate,
    mapLoader: IGroupLoader<OfflineMapCatalogItem>
) : IMapSortStrategy {

    private val sort = GroupSort(
        MapMinimumDistanceMapper(mapLoader, 100000f) { location }
    )

    override suspend fun sort(maps: List<OfflineMapCatalogItem>): List<OfflineMapCatalogItem> {
        return sort.sort(maps)
    }
}
