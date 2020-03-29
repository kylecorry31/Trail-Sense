package com.kylecorry.trail_sense.weather.altimeter

import android.hardware.SensorManager
import com.kylecorry.trail_sense.shared.AltitudeReading
import com.kylecorry.trail_sense.shared.PressureAltitudeReading

class InitialCalibrationBarometerAltitudeCalculator : IAltitudeCalculator {
    override fun convert(readings: List<PressureAltitudeReading>): List<AltitudeReading> {

        if (readings.isEmpty()) return listOf()

        val referenceReading = readings.first()

        return readings.map {
            val altitude = getCalibratedAltitude(referenceReading.altitude, referenceReading.pressure, it.pressure)
            AltitudeReading(it.time, altitude)
        }
    }

    private fun getCalibratedAltitude(gpsAltitude: Float, pressureAtGpsAltitude: Float, currentPressure: Float): Float {
        val gpsBarometricAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressureAtGpsAltitude)
        val currentBarometricAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressure)
        val change = currentBarometricAltitude - gpsBarometricAltitude
        return gpsAltitude + change
    }

}