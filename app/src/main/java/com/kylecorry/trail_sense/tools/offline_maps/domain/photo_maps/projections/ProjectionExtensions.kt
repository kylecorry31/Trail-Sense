package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections

import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance

fun IMapProjection.distancePerPixel(location1: Coordinate, location2: Coordinate): Distance? {
    val meters = location1.distanceTo(location2)
    val pixels = toPixels(location1).distanceTo(toPixels(location2))

    if (meters == 0f || pixels == 0f) return null

    return Distance.meters(meters / pixels)
}
