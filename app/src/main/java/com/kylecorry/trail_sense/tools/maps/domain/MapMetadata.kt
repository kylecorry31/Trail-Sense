package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.geology.CoordinateBounds

data class MapMetadata(
    val size: Size,
    val fileSize: Float,
    val projectionType: MapProjectionType = MapProjectionType.Mercator,
    val bounds: CoordinateBounds? = null
)