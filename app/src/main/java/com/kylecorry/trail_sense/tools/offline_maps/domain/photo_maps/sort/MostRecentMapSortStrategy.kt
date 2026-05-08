package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.sort

import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import com.kylecorry.trail_sense.shared.grouping.sort.GroupSort
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.sort.mappers.MapLatestIdMapper

class MostRecentMapSortStrategy(
    mapLoader: IGroupLoader<IMap>,
) : IMapSortStrategy {
    private val sort = GroupSort(
        MapLatestIdMapper(mapLoader),
        ascending = false
    )

    override suspend fun sort(maps: List<IMap>): List<IMap> {
        return sort.sort(maps)
    }
}
