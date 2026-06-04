package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geology.CoordinateBounds

class PhotoMapBoundsCalculator {

    fun calculate(map: PhotoMap): CoordinateBounds? {
        if (!map.isCalibrated) {
            return null
        }

        if (map.isFullWorld) {
            return CoordinateBounds.world
        }

        val size = map.unrotatedSize()
        val topLeft = map.projection.toCoordinate(Vector2(0f, 0f))
        val bottomLeft = map.projection.toCoordinate(Vector2(0f, size.height))
        val topRight = map.projection.toCoordinate(Vector2(size.width, 0f))
        val bottomRight = map.projection.toCoordinate(Vector2(size.width, size.height))

        return CoordinateBounds.from(listOf(topLeft, bottomLeft, topRight, bottomRight))
    }
}
