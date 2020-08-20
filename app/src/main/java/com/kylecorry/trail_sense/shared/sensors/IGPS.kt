package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.trail_sense.shared.domain.Coordinate

interface IGPS: ISensor, IAltimeter {
    val location: Coordinate
    val speed: Float
    val verticalAccuracy: Float?
    val horizontalAccuracy: Float?
}