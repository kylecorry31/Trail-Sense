package com.kylecorry.trail_sense.tools.maps.domain

data class PercentBounds(
    val topLeft: PercentCoordinate,
    val topRight: PercentCoordinate,
    val bottomLeft: PercentCoordinate,
    val bottomRight: PercentCoordinate,
) {
    fun toPixelBounds(width: Float, height: Float): PixelBounds {
        return PixelBounds(
            topLeft.toPixels(width, height),
            topRight.toPixels(width, height),
            bottomLeft.toPixels(width, height),
            bottomRight.toPixels(width, height)
        )
    }
}