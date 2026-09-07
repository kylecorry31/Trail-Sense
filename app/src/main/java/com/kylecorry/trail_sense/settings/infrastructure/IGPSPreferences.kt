package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.sensors.gps.GPSAccuracyFilter

interface IGPSPreferences {
    var useAutoLocation: Boolean
    val requiresSatelliteCount: Boolean
    val rejectInvalidReadings: Boolean
    val accuracyFilter: GPSAccuracyFilter
    val smoothing: Int
    val useNMEA: Boolean
    var locationOverride: Coordinate
    val hasLocationOverride: Boolean
}
