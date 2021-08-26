package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.domain.weather.PressureReading

interface ISeaLevelCalibrationStrategy {

    fun calibrate(readings: List<PressureAltitudeReading>): List<PressureReading>

}