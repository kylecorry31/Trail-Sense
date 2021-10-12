package com.kylecorry.trail_sense.shared.maps

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate

interface ICoordinateToPixelStrategy {
    fun getPixels(coordinate: Coordinate): PixelCoordinate
}