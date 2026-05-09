package com.kylecorry.trail_sense.tools.offline_maps.domain.sort

import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap

interface IMapSortStrategy {
    suspend fun sort(maps: List<IMap>): List<IMap>
}
