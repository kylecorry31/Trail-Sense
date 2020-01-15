package com.kylecorry.trail_sense.weather

interface IPressureCalibrator {
    fun getCalibratedPressure(rawPressure: Float, altitude: Float): Float
}