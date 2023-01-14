package com.kylecorry.trail_sense.tools.maps.infrastructure

import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapProjectionType

class MapService(private val repo: IMapRepo) {

    suspend fun setProjection(map: Map, projection: MapProjectionType): Map {
        val newMap = map.copy(metadata = map.metadata.copy(projection = projection))
        repo.addMap(newMap)
        return newMap
    }

}