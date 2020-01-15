package com.kylecorry.trail_sense.sensors.gps

import com.kylecorry.trail_sense.models.Coordinate
import com.kylecorry.trail_sense.sensors.altimeter.IAltimeter

interface IGPS : IAltimeter {
    val location: Coordinate
}