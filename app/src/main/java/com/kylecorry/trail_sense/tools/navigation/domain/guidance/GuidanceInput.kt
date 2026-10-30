package com.kylecorry.trail_sense.tools.navigation.domain.guidance

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance

data class GuidanceInput(
    val location: Coordinate,
    val elevation: Distance,
    val declination: Float,
    val useTrueNorth: Boolean
)
