package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.units.Coordinate

sealed class NavigationStrategy {
    class Bearing(
        val bearing: com.kylecorry.sol.units.Bearing,
        val isTrueNorth: Boolean,
        val declination: Float,
        val startingLocation: Coordinate? = null
    ) : NavigationStrategy()

    class Beacon(
        val beacon: com.kylecorry.trail_sense.tools.beacons.domain.Beacon
    ) : NavigationStrategy()
}