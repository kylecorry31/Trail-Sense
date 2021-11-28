package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate

interface IMapCoordinateConverter {
    fun toCoordinate(pixel: PixelCoordinate): Coordinate?
    fun toPixels(coordinate: Coordinate): PixelCoordinate?
}