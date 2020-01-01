package com.kylecorry.trail_sense.navigator.gps

import android.location.Location
import com.kylecorry.trail_sense.navigator.normalizeAngle
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
        val results = FloatArray(3)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)
        return normalizeAngle(results[1])
    }


    /**
     * Get the distance in km between two coordinates
     * @param from the starting coordinate
     * @param to the ending coordinate
     * @return the distance in meters between the two coordinates
     */
    fun getDistance(from: Coordinate, to: Coordinate): Float {
        val results = FloatArray(3)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)
        return results[0]
    }

    /**
     * Converts meters to feet
     */
    private fun convertMetersToFeet(meters: Float): Float {
        return meters * 3.28084f
    }

    /**
     * Converts feet to miles
     */
    private fun convertFeetToMiles(feet: Float): Float {
        return feet / 5280f
    }

    fun convertToBaseUnit(meters: Float, units: String): Float {
        return if (units == "feet_miles"){
            convertMetersToFeet(meters)
        } else {
            meters
        }
    }

    /**
     * Converts a distance in meters to a readable string in the given unit system
     */
    fun distanceToReadableString(meters: Float, units: String): String {
        if (units == "feet_miles"){
            val feetThreshold = 500
            val feet =
                convertMetersToFeet(meters)
            return if (feet >= feetThreshold) {
                // Display as miles
                "${round(
                    convertFeetToMiles(
                        feet
                    ) * 100f) / 100f} mi"
            } else {
                // Display as feet
                "${feet.roundToInt()} ft"
            }
        } else {
            val meterThreshold = 200
            return if (meters >= meterThreshold) {
                // Display as km
                val km = meters / 1000f
                "${round( km * 100f) / 100f} km"
            } else {
                // Display as meters
                "${meters.roundToInt()} m"
            }
        }
    }
}