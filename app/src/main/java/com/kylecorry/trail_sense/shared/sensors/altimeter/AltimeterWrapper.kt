package com.kylecorry.trail_sense.shared.sensors.altimeter

import com.kylecorry.andromeda.core.sensors.IAltimeter

interface AltimeterWrapper: IAltimeter {
    val altimeter: IAltimeter
    val altitudeAccuracy: Float?
}