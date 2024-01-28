package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.UserPreferences

// TODO: Remove this

/**
 * A helper object for coordinate related math
 */
object LocationMath {


    private fun convertUnitPerSecondsToUnitPerHours(unitPerSecond: Float): Float {
        return unitPerSecond * 60 * 60
    }

    fun convertToBaseSpeed(metersPerSecond: Float, units: UserPreferences.DistanceUnits): Float {
        return if (units == UserPreferences.DistanceUnits.Feet) {
            convertUnitPerSecondsToUnitPerHours(
                Distance(
                    metersPerSecond,
                    DistanceUnits.Meters
                ).convertTo(DistanceUnits.Miles).distance
            )
        } else {
            convertUnitPerSecondsToUnitPerHours(
                Distance(
                    metersPerSecond,
                    DistanceUnits.Meters
                ).convertTo(DistanceUnits.Kilometers).distance
            )
        }
    }

}