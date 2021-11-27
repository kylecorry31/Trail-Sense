package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.sol.science.geology.CoordinateBounds

data class MapMetadata(
    val width: Float,
    val height: Float,
    val fileSize: Float,
    val bounds: CoordinateBounds? = null
)