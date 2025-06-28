package com.kylecorry.trail_sense.tools.photo_maps.domain.projections

import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance

/**
 * Calculates the distance per pixel between two locations
 * @return the distance per pixel or null if it cannot be calculated
 */
fun IMapProjection.distancePerPixel(location1: Coordinate, location2: Coordinate): Distance? {
    val meters = location1.distanceTo(location2)
    val pixels = toPixels(location1).distanceTo(toPixels(location2))

    // Unable to calculate
    if (meters == 0f || pixels == 0f) return null

    return Distance.meters(meters / pixels)
}