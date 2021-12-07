package com.kylecorry.trail_sense.tools.maps.infrastructure

import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapProjectionType

class MapService(private val repo: IMapRepo) {

    suspend fun setProjection(map: Map, projection: MapProjectionType) {
        repo.addMap(map.copy(projection = projection))
    }

}