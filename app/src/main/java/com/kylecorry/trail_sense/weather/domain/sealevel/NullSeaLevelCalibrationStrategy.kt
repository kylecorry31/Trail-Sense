package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.domain.weather.PressureReading

class NullSeaLevelCalibrationStrategy: ISeaLevelCalibrationStrategy {
    override fun calibrate(readings: List<PressureAltitudeReading>): List<PressureReading> {
        return readings.map { it.pressureReading() }
    }
}