package com.kylecorry.trail_sense.shared

import com.kylecorry.sol.units.Coordinate

data class Position(val location: Coordinate, val altitude: Float, val bearing: Float, val speed: Float = 0f)