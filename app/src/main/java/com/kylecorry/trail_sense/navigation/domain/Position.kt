package com.kylecorry.trail_sense.navigation.domain

import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.domain.Coordinate

data class Position(val location: Coordinate, val altitude: Float, val bearing: Bearing, val speed: Float = 0f)