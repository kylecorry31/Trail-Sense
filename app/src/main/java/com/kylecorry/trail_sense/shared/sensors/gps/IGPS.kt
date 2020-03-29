package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.trail_sense.shared.Coordinate
import com.kylecorry.trail_sense.shared.sensors.altimeter.IAltimeter

interface IGPS : IAltimeter {
    val location: Coordinate
}