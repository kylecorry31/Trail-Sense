package com.kylecorry.trail_sense.tools.maps.domain.sort

import com.kylecorry.trail_sense.shared.grouping.sort.GroupSort
import com.kylecorry.trail_sense.tools.maps.domain.IMap
import com.kylecorry.trail_sense.tools.maps.domain.sort.mappers.MapNameMapper

class NameMapSortStrategy : IMapSortStrategy {
    private val sort = GroupSort(MapNameMapper())

    override suspend fun sort(maps: List<IMap>): List<IMap> {
        return sort.sort(maps)
    }
}