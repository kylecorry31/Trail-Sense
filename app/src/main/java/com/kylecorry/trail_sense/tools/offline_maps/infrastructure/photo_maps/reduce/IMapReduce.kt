package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.reduce

import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap

interface IMapReduce {

    suspend fun reduce(map: PhotoMap)

}
