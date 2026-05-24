package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Speed

data class NavigationSensorValues(
    val location: Coordinate,
    val locationAccuracy: Distance?,
    val elevation: Distance,
    val elevationAccuracy: Distance?,
    val bearing: Bearing,
    val declination: Float,
    val speed: Speed,
    val gpsSpeed: Speed,
    val gps: IGPS? = null,
    val compass: ICompass? = null
)
