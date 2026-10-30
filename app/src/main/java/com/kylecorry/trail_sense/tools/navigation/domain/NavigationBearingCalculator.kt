package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.sol.units.Coordinate

object NavigationBearingCalculator {

    fun getDeclination(
        location: Coordinate,
        elevation: Float,
        time: Long,
        useAutoDeclination: Boolean,
        declinationOverride: Float
    ): Float {
        return if (useAutoDeclination) {
            Geophysics.getGeomagneticDeclination(location, elevation, time)
        } else {
            declinationOverride
        }
    }
}
