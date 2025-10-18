package com.kylecorry.trail_sense.tools.ballistics.domain

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Speed

data class TrajectoryPoint(
    val time: Float,
    val distance: Distance,
    val velocity: Speed,
    val drop: Distance
)