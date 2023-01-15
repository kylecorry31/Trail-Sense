package com.kylecorry.trail_sense.tools.maps.domain.sort

import com.kylecorry.trail_sense.tools.maps.domain.IMap

interface IMapSortStrategy {
    suspend fun sort(maps: List<IMap>): List<IMap>
}