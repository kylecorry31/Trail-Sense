package com.kylecorry.survival_aid.navigation

import kotlin.math.*

/**
 * A helper object for coordinate related math
 */
object CoordinateMath {

    /**
     * Get the bearing between two coordinates
     * @param from the starting coordinate
     * @param to the ending coordinate
     * @return the bearing in degrees (same as Compass.azimuth)
     */
    fun getBearing(from: Coordinate, to: Coordinate): Float {
        val deltaLongitude = toRadians(to.longitude - from.longitude)
        val toLat = toRadians(to.latitude)
        val fromLat = toRadians(from.latitude)
        val x = cos(toLat) * sin(deltaLongitude)

        val y = cos(fromLat) * sin(toLat) - sin(fromLat) * cos(toLat) * cos(deltaLongitude)

        val bearing = atan2(x, y)
        return toDegrees(bearing)
    }


    /**
     * Get the distance in km between two coordinates
     * @param from the starting coordinate
     * @param to the ending coordinate
     * @return the distance in km between the two coordinates
     */
    fun getDistance(from: Coordinate, to: Coordinate): Float {
        val earthRadius = 6372.8f
        val fromLat = toRadians(from.latitude)
        val toLat = toRadians(to.latitude)
        val deltaLat = toRadians(to.latitude - from.latitude)
        val deltaLon = toRadians(to.longitude - from.longitude)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(fromLat) * cos(toLat) * sin(deltaLon / 2) * sin(deltaLon / 2)

        return earthRadius * 2 * asin(sqrt(a))
    }


    private fun toRadians(deg: Float): Float {
        return Math.toRadians(deg.toDouble()).toFloat()
    }

    private fun toDegrees(rad: Float): Float {
        return Math.toDegrees(rad.toDouble()).toFloat()
    }

}