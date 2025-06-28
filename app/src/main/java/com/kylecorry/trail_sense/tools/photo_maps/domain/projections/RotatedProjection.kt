package com.kylecorry.trail_sense.tools.photo_maps.domain.projections

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.rotateInRect

class RotatedProjection(
    private val projection: IMapProjection,
    private val size: Size,
    private val rotation: Float
) : IMapProjection {

    private val rotatedSize = size.rotate(rotation)

    override fun toCoordinate(pixel: Vector2): Coordinate {
        val rotated = pixel.rotateInRect(rotation, size, rotatedSize)
        return projection.toCoordinate(rotated)
    }

    override fun toPixels(location: Coordinate): Vector2 {
        val unrotated = projection.toPixels(location)
        return unrotated.rotateInRect(-rotation, rotatedSize, size)
    }
}