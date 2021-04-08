package com.kylecorry.trail_sense.tools.maps.domain

data class PixelCircle(val center: PixelCoordinate, val radius: Float){
    fun contains(pixel: PixelCoordinate): Boolean {
        val distance = center.distanceTo(pixel)
        return distance <= radius
    }
}
