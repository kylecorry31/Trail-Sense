package com.kylecorry.trail_sense.tools.photo_maps.domain.sort

import com.kylecorry.trail_sense.tools.photo_maps.domain.IMap

interface IMapSortStrategy {
    suspend fun sort(maps: List<IMap>): List<IMap>
}