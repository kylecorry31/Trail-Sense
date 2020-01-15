package com.kylecorry.trail_sense.navigator

import com.kylecorry.trail_sense.models.AltitudeReading
import com.kylecorry.trail_sense.models.Coordinate

interface IDeclinationCalculator {
    fun calculateDeclination(location: Coordinate, altitude: AltitudeReading): Float
}