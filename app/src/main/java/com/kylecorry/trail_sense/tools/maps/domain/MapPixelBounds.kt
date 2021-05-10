package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.trailsensecore.domain.pixels.PixelCoordinate

data class MapPixelBounds(
    val topLeft: PixelCoordinate,
    val topRight: PixelCoordinate,
    val bottomLeft: PixelCoordinate,
    val bottomRight: PixelCoordinate,
)