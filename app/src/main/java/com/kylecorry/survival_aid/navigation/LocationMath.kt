package com.kylecorry.survival_aid.navigation

import android.location.Location
import kotlin.math.*

/**
 * A helper object for coordinate related math
 */
object LocationMath {

    /**
     * Get the bearing between two coordinates
     * @param from the starting coordinate
     * @param to the ending coordinate
     * @return the bearing in degrees (same as Compass.azimuth)
     */
    fun getBearing(from: Coordinate, to: Coordinate): Float {
        val locationA = Location("point A")

        locationA.latitude = from.latitude.toDouble()
        locationA.longitude = from.longitude.toDouble()

        val locationB = Location("point B")

        locationB.latitude = to.latitude.toDouble()
        locationB.longitude = to.longitude.toDouble()

        var bearing = locationA.bearingTo(locationB)

        if (bearing < 0){
            bearing += 360
        }
        return bearing % 360
    }


    /**
     * Get the distance in km between two coordinates
     * @param from the starting coordinate
     * @param to the ending coordinate
     * @return the distance in meters between the two coordinates
     */
    fun getDistance(from: Coordinate, to: Coordinate): Float {
        val locationA = Location("point A")

        locationA.latitude = from.latitude.toDouble()
        locationA.longitude = from.longitude.toDouble()

        val locationB = Location("point B")

        locationB.latitude = to.latitude.toDouble()
        locationB.longitude = to.longitude.toDouble()

        return locationA.distanceTo(locationB)
    }

    /**
     * Converts meters to feet
     */
    fun convertMetersToFeet(meters: Float): Float {
        return meters * 3.28084f
    }

    /**
     * Converts feet to miles
     */
    fun convertFeetToMiles(feet: Float): Float {
        return feet / 5280f
    }

    /**
     * Converts miles to feet
     */
    fun convertMilesToFeet(miles: Float): Float {
        return miles * 5280
    }

    /**
     * Converts a distance in meters to a readable string in the given unit system
     */
    fun distanceToReadableString(meters: Float, unitSystem: UnitSystem): String {
        if (unitSystem == UnitSystem.IMPERIAL){
            val feetThreshold = 500
            val feet = convertMetersToFeet(meters)
            return if (feet >= feetThreshold) {
                // Display as miles
                "${round(convertFeetToMiles(feet) / 100f) * 100f} mi"
            } else {
                // Display as feet
                "${feet.roundToInt()} ft"
            }
        } else {
            val meterThreshold = 200
            return if (meters >= meterThreshold) {
                // Display as km
                val km = meters / 1000f
                "${round( km / 100f) * 100f} km"
            } else {
                // Display as meters
                "${meters.roundToInt()} m"
            }
        }
    }


    private fun toRadians(deg: Float): Float {
        return Math.toRadians(deg.toDouble()).toFloat()
    }

    private fun toDegrees(rad: Float): Float {
        return Math.toDegrees(rad.toDouble()).toFloat()
    }

}

enum class UnitSystem {
    METRIC, IMPERIAL
}