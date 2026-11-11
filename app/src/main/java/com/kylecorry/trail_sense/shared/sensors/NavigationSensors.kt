package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.trail_sense.shared.sensors.compass.INorthReferencedCompass
import com.kylecorry.trail_sense.shared.sensors.gps.ISatelliteGPS

class NavigationSensors(
    val values: NavigationSensorValues,
    val gps: ISatelliteGPS,
    val compass: INorthReferencedCompass,
    val altimeter: IAltimeter
)
