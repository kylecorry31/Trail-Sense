package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils

object NavigationBearingCalculator {

    fun getBearing(
        myLocation: Coordinate,
        destination: Destination,
        declination: Float,
        useTrueNorth: Boolean
    ): Bearing {
        val target = when (destination) {
            is Destination.Path -> destination.route.guidance.target
            is Destination.Beacon -> destination.beacon.coordinate
            is Destination.Bearing -> {
                if (destination.startingLocation == null) {
                    // True north setting is intentionally ignored, it will retain the recorded bearing on all compasses
                    return destination.bearing
                }
                destination.targetLocation!!
            }
        }

        return fromTrueNorth(myLocation.bearingTo(target), useTrueNorth, declination)
    }

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

    private fun fromTrueNorth(
        bearing: Bearing,
        useTrueNorth: Boolean,
        declination: Float
    ): Bearing {
        if (useTrueNorth) {
            return bearing
        }
        return DeclinationUtils.fromTrueNorthBearing(bearing, declination)
    }
}
