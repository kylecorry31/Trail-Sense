package com.kylecorry.trail_sense.tools.maps.infrastructure.reduce

import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap

interface IMapReduce {

    suspend fun reduce(map: PhotoMap)

}