package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.colors.AppColor

sealed class Destination {
    class Bearing(
        val bearing: com.kylecorry.sol.units.Bearing,
        val isTrueNorth: Boolean,
        val declination: Float,
        val startingLocation: Coordinate? = null
    ) : Destination() {

        val trueBearing: com.kylecorry.sol.units.Bearing
            get() {
                return if (isTrueNorth) {
                    bearing
                } else {
                    bearing.withDeclination(declination)
                }
            }

        val targetLocation: Coordinate?
            get() {
                return startingLocation?.plus(bearingDistance, trueBearing)
            }

        companion object {
            val bearingDistance: Distance = Distance.kilometers(80f).meters()
            val defaultColor: Int = AppColor.Blue.color
        }
    }

    class Beacon(
        val beacon: com.kylecorry.trail_sense.tools.beacons.domain.Beacon
    ) : Destination()
}