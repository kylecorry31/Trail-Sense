package com.kylecorry.trail_sense.shared.sensors2

import com.kylecorry.trail_sense.shared.Coordinate

interface IGPS: ISensor, IAltimeter {
    val location: Coordinate
}