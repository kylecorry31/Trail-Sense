package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.sol.math.SolMath.map
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate

class EquirectangularProjection(
    private val bounds: CoordinateBounds,
    private val mapSize: Size
) : IMapProjection {

    override fun toCoordinate(pixel: Vector2): Coordinate {
        val longitude =
            map(pixel.x.toDouble(), 0.0, mapSize.width.toDouble(), bounds.west, bounds.east)
        val latitude = map(
            mapSize.height - pixel.y.toDouble(),
            0.0,
            mapSize.height.toDouble(),
            bounds.south,
            bounds.north
        )

        return Coordinate(latitude, longitude)
    }

    override fun toPixels(coordinate: Coordinate): Vector2 {
        val x = map(
            coordinate.longitude,
            bounds.west,
            bounds.east,
            0.0,
            mapSize.width.toDouble()
        ).toFloat()
        val y = mapSize.height - map(
            coordinate.latitude,
            bounds.south,
            bounds.north,
            0.0,
            mapSize.height.toDouble()
        ).toFloat()
        return Vector2(x, y)
    }
}