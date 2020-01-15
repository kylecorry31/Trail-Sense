package com.kylecorry.trail_sense.sensors.compass

import com.kylecorry.trail_sense.models.Bearing

interface ICompass {
    val azimuth: Bearing
}