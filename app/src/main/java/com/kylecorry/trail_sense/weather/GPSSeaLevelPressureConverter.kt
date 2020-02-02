package com.kylecorry.trail_sense.weather

import com.kylecorry.trail_sense.models.PressureAltitudeReading
import com.kylecorry.trail_sense.models.PressureReading

class GPSSeaLevelPressureConverter : ISeaLevelPressureConverter {

    override fun convert(readings: List<PressureAltitudeReading>): List<PressureReading> {
        return readings.map { PressureReading(it.time, SeaLevelPressureCalibrator.calibrate(it.pressure, it.altitude)) }
    }

}