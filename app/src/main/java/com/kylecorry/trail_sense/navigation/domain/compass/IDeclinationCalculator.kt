package com.kylecorry.trail_sense.navigation.domain.compass

import com.kylecorry.trail_sense.shared.Coordinate

interface IDeclinationCalculator {
    fun calculate(location: Coordinate, altitude: Float): Float
}