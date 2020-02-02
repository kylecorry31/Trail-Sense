package com.kylecorry.trail_sense.weather

import com.kylecorry.trail_sense.models.PressureAltitudeReading
import com.kylecorry.trail_sense.models.PressureReading

class GPSSeaLevelPressureConverter : ISeaLevelPressureConverter {

    override fun convert(readings: List<PressureAltitudeReading>): List<PressureReading> {
        val output = mutableListOf<PressureReading>()

        for (reading in readings){
            val calibratedPressure = SeaLevelPressureCalibrator.calibrate(reading.pressure, reading.altitude)
            output.add(PressureReading(reading.time, calibratedPressure))
        }

        return output
    }

}