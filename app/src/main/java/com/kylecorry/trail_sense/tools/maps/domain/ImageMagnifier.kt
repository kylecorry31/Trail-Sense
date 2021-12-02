package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Size

class ImageMagnifier(private val imageSize: Size, private val magnifierSize: Size) {

    fun getMagnifierPosition(tapPosition: PixelCoordinate): PixelCoordinate {

        val x = if (tapPosition.x > imageSize.width / 2) {
            0f
        } else {
            imageSize.width - magnifierSize.width
        }

        return PixelCoordinate(x, 0f)
    }

}