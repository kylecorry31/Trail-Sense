package com.kylecorry.trail_sense.tools.maps.infrastructure.reduce

import com.kylecorry.trail_sense.tools.maps.domain.Map

interface IMapReduce {

    suspend fun reduce(map: Map)

}