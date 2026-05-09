package com.kylecorry.trail_sense.tools.offline_maps.domain.sort

import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import com.kylecorry.trail_sense.shared.grouping.sort.GroupSort
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.sort.mappers.MapCreateTimeMapper

class MostRecentMapSortStrategy(
    mapLoader: IGroupLoader<IMap>,
) : IMapSortStrategy {
    private val sort = GroupSort(
        MapCreateTimeMapper(mapLoader),
        ascending = false
    )

    override suspend fun sort(maps: List<IMap>): List<IMap> {
        return sort.sort(maps)
    }
}
