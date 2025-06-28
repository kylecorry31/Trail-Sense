package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.reduce

import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap

interface IMapReduce {

    suspend fun reduce(map: PhotoMap)

}