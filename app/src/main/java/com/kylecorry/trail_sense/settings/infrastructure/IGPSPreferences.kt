package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.sensors.gps.GPSAccuracyRequirement

interface IGPSPreferences {
    var useAutoLocation: Boolean
    val requiresSatellites: Boolean
    val filterLocationReadings: Boolean
    val accuracyRequirement: GPSAccuracyRequirement
    val useFilteredGPS: Boolean
    val useNMEA: Boolean
    var locationOverride: Coordinate
    val hasLocationOverride: Boolean
}
