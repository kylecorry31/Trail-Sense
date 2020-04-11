package com.kylecorry.trail_sense.navigation.domain.compass

import com.kylecorry.trail_sense.shared.Coordinate

interface IDeclinationCalculator {
    fun calculateDeclination(location: Coordinate, altitude: Float): Float
}