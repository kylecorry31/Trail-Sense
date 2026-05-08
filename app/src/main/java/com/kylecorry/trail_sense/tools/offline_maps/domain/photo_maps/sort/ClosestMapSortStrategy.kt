package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.sort

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import com.kylecorry.trail_sense.shared.grouping.sort.GroupSort
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.sort.mappers.MapMinimumDistanceMapper

class ClosestMapSortStrategy(
    location: Coordinate,
    mapLoader: IGroupLoader<IMap>
) : IMapSortStrategy {

    private val sort = GroupSort(
        MapMinimumDistanceMapper(mapLoader, 100000f) { location }
    )

    override suspend fun sort(maps: List<IMap>): List<IMap> {
        return sort.sort(maps)
    }
}
