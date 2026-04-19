package com.kylecorry.trail_sense.shared.domain

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.UserPreferences

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
                Distance.Companion.from(
                    metersPerSecond,
                    DistanceUnits.Meters
                ).convertTo(DistanceUnits.Miles).value
            )
        } else {
            convertUnitPerSecondsToUnitPerHours(
                Distance.Companion.from(
                    metersPerSecond,
                    DistanceUnits.Meters
                ).convertTo(DistanceUnits.Kilometers).value
            )
        }
    }

}
