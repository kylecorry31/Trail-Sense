package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.science.geology.IGeologyService
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.toPixel
import com.kylecorry.trail_sense.shared.toVector2

class MercatorProjection(
    private val bounds: CoordinateBounds,
    private val size: Pair<Float, Float>,
    private val geology: IGeologyService = GeologyService()
) : IProjection {

    override fun toCoordinate(pixel: PixelCoordinate): Coordinate {
        return geology.fromMercator(pixel.toVector2(), bounds, size)
    }

    override fun toPixels(coordinate: Coordinate): PixelCoordinate {
        val coords = geology.toMercator(coordinate, bounds, size)
        return coords.toPixel()
    }
}