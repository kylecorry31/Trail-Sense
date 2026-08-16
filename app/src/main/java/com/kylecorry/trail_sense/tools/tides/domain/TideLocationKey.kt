package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.units.Coordinate

object TideLocationKey {

    private const val PRECISION = 2

    fun of(location: Coordinate): Coordinate {
        return Coordinate(
            location.latitude.roundPlaces(PRECISION),
            location.longitude.roundPlaces(PRECISION)
        )
    }
}
