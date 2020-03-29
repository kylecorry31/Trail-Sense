package com.kylecorry.trail_sense.weather.sealevel

import com.kylecorry.trail_sense.shared.PressureAltitudeReading
import com.kylecorry.trail_sense.shared.PressureReading

class GPSSeaLevelPressureConverter :
    ISeaLevelPressureConverter {

    override fun convert(readings: List<PressureAltitudeReading>): List<PressureReading> {
        return readings.map {
            PressureReading(
                it.time,
                SeaLevelPressureCalibrator.calibrate(
                    it.pressure,
                    it.altitude
                )
            )
        }
    }

}