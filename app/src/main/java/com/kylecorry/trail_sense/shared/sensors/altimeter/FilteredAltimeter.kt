package com.kylecorry.trail_sense.shared.sensors.altimeter

import com.kylecorry.andromeda.core.sensors.IAltimeter

interface FilteredAltimeter: IAltimeter {
    val altimeter: IAltimeter
    val altitudeAccuracy: Float?
}