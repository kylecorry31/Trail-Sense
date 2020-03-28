package com.kylecorry.trail_sense.weather.altimeter

import android.hardware.SensorManager
import com.kylecorry.trail_sense.models.AltitudeReading
import com.kylecorry.trail_sense.models.PressureAltitudeReading

class BarometerAltitudeCalculator : IAltitudeCalculator {
    override fun convert(readings: List<PressureAltitudeReading>): List<AltitudeReading> {
        return readings.map { AltitudeReading(it.time, SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, it.pressure)) }
    }
}