package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.andromeda.core.units.PixelCoordinate

data class PercentCoordinate(val x: Float, val y: Float) {
    fun toPixels(width: Float, height: Float): PixelCoordinate {
        return PixelCoordinate(x * width, y * height)
    }

    fun toPixels(width: Int, height: Int): PixelCoordinate {
        return toPixels(width.toFloat(), height.toFloat())
    }

    fun rotate(rotation: Int): PercentCoordinate {
        val realRotation = if (rotation < 0) {
            (rotation % 360) + 360
        } else {
            rotation % 360
        }

        if (realRotation == 0) {
            return this
        }

        // Determine if X and Y are flipped
        val swapXY = realRotation == 90 || realRotation == 270

        // Determine if it should invert
        val invertX = realRotation == 90 || realRotation == 180
        val invertY = realRotation == 180 || realRotation == 270

        val newX = if (swapXY) y else x
        val newY = if (swapXY) x else y
        return PercentCoordinate(
            if (invertX) 1f - newX else newX,
            if (invertY) 1f - newY else newY
        )
    }
}
