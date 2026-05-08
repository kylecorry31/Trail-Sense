package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.sort

import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.IMap

interface IMapSortStrategy {
    suspend fun sort(maps: List<IMap>): List<IMap>
}
