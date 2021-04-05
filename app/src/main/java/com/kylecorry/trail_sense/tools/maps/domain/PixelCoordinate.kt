package com.kylecorry.trail_sense.tools.maps.domain

import kotlin.math.pow
import kotlin.math.sqrt

data class PixelCoordinate(val x: Float, val y: Float) {
    fun distanceTo(other: PixelCoordinate): Float {
        return sqrt((other.y - y).pow(2) + (other.x - x).pow(2))
    }
}
