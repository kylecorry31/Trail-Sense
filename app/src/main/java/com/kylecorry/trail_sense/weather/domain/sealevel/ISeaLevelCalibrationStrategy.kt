package com.kylecorry.trail_sense.weather.domain.sealevel

interface ISeaLevelCalibrationStrategy {

    fun calibrate(readings: List<com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading>): List<com.kylecorry.trailsensecore.domain.weather.PressureReading>

}