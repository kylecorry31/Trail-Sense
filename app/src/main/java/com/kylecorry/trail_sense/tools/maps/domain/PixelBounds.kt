package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.andromeda.core.units.PixelCoordinate

data class PixelBounds(
    val topLeft: PixelCoordinate,
    val topRight: PixelCoordinate,
    val bottomLeft: PixelCoordinate,
    val bottomRight: PixelCoordinate,
)