package com.kylecorry.trail_sense.shared

import com.kylecorry.andromeda.core.units.Bearing
import com.kylecorry.andromeda.core.units.Coordinate

data class Position(val location: Coordinate, val altitude: Float, val bearing: Bearing, val speed: Float = 0f)