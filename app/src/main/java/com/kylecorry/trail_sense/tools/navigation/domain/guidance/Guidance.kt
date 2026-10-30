package com.kylecorry.trail_sense.tools.navigation.domain.guidance

import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Distance

data class Guidance(
    val targetBearing: Bearing,
    val remainingDistance: Distance? = null,
    val traveledDistance: Distance? = null,
    val offRouteDistance: Distance? = null,
    val remainingElevationGain: Distance? = null,
    val remainingElevationLoss: Distance? = null,
    val progress: Float? = null,
    val arrived: Boolean = false
)
