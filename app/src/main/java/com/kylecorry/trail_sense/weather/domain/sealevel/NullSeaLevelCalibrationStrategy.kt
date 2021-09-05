package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading
import com.kylecorry.trail_sense.weather.domain.PressureReading

class NullSeaLevelCalibrationStrategy: ISeaLevelCalibrationStrategy {
    override fun calibrate(readings: List<PressureAltitudeReading>): List<PressureReading> {
        return readings.map { it.pressureReading() }
    }
}