package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.sort

import com.kylecorry.trail_sense.shared.grouping.sort.GroupSort
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.sort.mappers.MapNameMapper

class NameMapSortStrategy : IMapSortStrategy {
    private val sort = GroupSort(MapNameMapper())

    override suspend fun sort(maps: List<IMap>): List<IMap> {
        return sort.sort(maps)
    }
}
