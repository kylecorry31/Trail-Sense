package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.sensors.gps.GPSAccuracyFilter
import com.kylecorry.trail_sense.shared.sensors.gps.GPSLocationSource
import com.kylecorry.trail_sense.shared.sensors.gps.GPSPowerMode

interface IGPSPreferences {
    var locationSource: GPSLocationSource
    var accuracyFilter: GPSAccuracyFilter
    val smoothing: Int
    val powerMode: GPSPowerMode
    var locationOverride: Coordinate
    val hasLocationOverride: Boolean
}
