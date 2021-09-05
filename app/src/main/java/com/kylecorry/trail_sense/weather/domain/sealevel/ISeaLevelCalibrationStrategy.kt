package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading
import com.kylecorry.trail_sense.weather.domain.PressureReading

interface ISeaLevelCalibrationStrategy {

    fun calibrate(readings: List<PressureAltitudeReading>): List<PressureReading>

}