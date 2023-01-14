package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.sol.math.geometry.Size

data class MapMetadata(
    val size: Size,
    val fileSize: Long,
    val projection: MapProjectionType = MapProjectionType.Mercator
)