package com.kylecorry.trail_sense.navigation.domain.compass

import android.hardware.GeomagneticField
import com.kylecorry.trail_sense.shared.Coordinate

class DeclinationCalculator : IDeclinationCalculator {
    override fun calculate(location: Coordinate, altitude: Float): Float {
        val time: Long = System.currentTimeMillis()
        val geoField = GeomagneticField(location.latitude.toFloat(), location.longitude.toFloat(), altitude, time)
        return geoField.declination
    }
}