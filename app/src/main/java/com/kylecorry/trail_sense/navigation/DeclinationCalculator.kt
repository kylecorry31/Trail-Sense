package com.kylecorry.trail_sense.navigation

import android.hardware.GeomagneticField
import com.kylecorry.trail_sense.shared.AltitudeReading
import com.kylecorry.trail_sense.shared.Coordinate

class DeclinationCalculator :
    IDeclinationCalculator {
    override fun calculateDeclination(location: Coordinate, altitude: AltitudeReading): Float {
        val time: Long = System.currentTimeMillis() //altitude.time.toEpochMilli()
        val geoField = GeomagneticField(location.latitude.toFloat(), location.longitude.toFloat(), altitude.value, time)
        return geoField.declination
    }
}