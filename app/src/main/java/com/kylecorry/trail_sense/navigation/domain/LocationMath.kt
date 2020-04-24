package com.kylecorry.trail_sense.navigation.domain

import android.location.Location
import com.kylecorry.trail_sense.shared.math.normalizeAngle
import com.kylecorry.trail_sense.shared.Coordinate
import kotlin.math.*

/**
 * A helper object for coordinate related math
 */
object LocationMath {

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
            convertMetersToFeet(
                meters
            )
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
                convertMetersToFeet(
                    meters
                )
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