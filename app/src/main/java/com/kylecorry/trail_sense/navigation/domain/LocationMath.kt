package com.kylecorry.trail_sense.navigation.domain

import com.kylecorry.trail_sense.shared.UserPreferences
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * A helper object for coordinate related math
 */
object LocationMath {

    fun convert(distance: Float, fromUnits: DistanceUnits, toUnits: DistanceUnits): Float {
        val meters = convertToMeters(distance, fromUnits)
        return when(toUnits){
            DistanceUnits.Meters -> meters
            DistanceUnits.Kilometers -> convertMetersToKilometers(meters)
            DistanceUnits.Feet -> convertMetersToFeet(meters)
            DistanceUnits.Miles -> convertFeetToMiles(convertMetersToFeet(meters))
        }
    }

    fun convertToMeters(distance: Float, fromUnits: DistanceUnits): Float {
        return when(fromUnits){
            DistanceUnits.Meters -> distance
            DistanceUnits.Kilometers -> distance * 1000
            DistanceUnits.Miles -> convertFeetToMeters(distance * 5280f)
            DistanceUnits.Feet -> convertFeetToMeters(distance)
        }
    }

    /**
     * Converts meters to feet
     */
    private fun convertMetersToFeet(meters: Float): Float {
        return meters * 3.28084f
    }

    /**
     * Converts feet to meters
     */
    private fun convertFeetToMeters(feet: Float): Float {
        return feet / 3.28084f
    }

    /**
     * Converts feet to miles
     */
    private fun convertFeetToMiles(feet: Float): Float {
        return feet / 5280f
    }

    private fun convertUnitPerSecondsToUnitPerHours(unitPerSecond: Float): Float {
        return unitPerSecond * 60 * 60
    }

    private fun convertMetersToKilometers(meters: Float): Float {
        return meters / 1000f
    }

    fun convertToBaseUnit(meters: Float, units: UserPreferences.DistanceUnits): Float {
        return if (units == UserPreferences.DistanceUnits.Feet) {
            convertMetersToFeet(meters)
        } else {
            meters
        }
    }

    fun convertToBaseSpeed(metersPerSecond: Float, units: UserPreferences.DistanceUnits): Float {
        return if (units == UserPreferences.DistanceUnits.Feet) {
            convertUnitPerSecondsToUnitPerHours(
                convertFeetToMiles(
                    convertMetersToFeet(
                        metersPerSecond
                    )
                )
            )
        } else {
            convertUnitPerSecondsToUnitPerHours(convertMetersToKilometers(metersPerSecond))
        }
    }

    fun convertToMeters(distance: Float, units: UserPreferences.DistanceUnits): Float {
        return if (units == UserPreferences.DistanceUnits.Meters) {
            distance
        } else {
            convertFeetToMeters(distance)
        }
    }

    fun distanceToReadableString(meters: Float, units: UserPreferences.DistanceUnits): String {
        if (units == UserPreferences.DistanceUnits.Feet) {
            val feetThreshold = 1000
            val feet =
                convertMetersToFeet(
                    meters
                )
            return if (feet >= feetThreshold) {
                // Display as miles
                "${round(
                    convertFeetToMiles(
                        feet
                    ) * 100f
                ) / 100f} mi"
            } else {
                // Display as feet
                "${feet.roundToInt()} ft"
            }
        } else {
            val meterThreshold = 999
            return if (meters >= meterThreshold) {
                // Display as km
                val km = meters / 1000f
                "${round(km * 100f) / 100f} km"
            } else {
                // Display as meters
                "${meters.roundToInt()} m"
            }
        }
    }

    /**
     * Converts a distance in meters to a readable string in the given unit system
     */
    fun distanceToReadableString(meters: Float, units: String): String {
        if (units == "feet_miles") {
            val feetThreshold = 1000
            val feet =
                convertMetersToFeet(
                    meters
                )
            return if (feet >= feetThreshold) {
                // Display as miles
                "${round(
                    convertFeetToMiles(
                        feet
                    ) * 100f
                ) / 100f} mi"
            } else {
                // Display as feet
                "${feet.roundToInt()} ft"
            }
        } else {
            val meterThreshold = 999
            return if (meters >= meterThreshold) {
                // Display as km
                val km = meters / 1000f
                "${round(km * 100f) / 100f} km"
            } else {
                // Display as meters
                "${meters.roundToInt()} m"
            }
        }
    }
}