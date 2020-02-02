package com.kylecorry.trail_sense.weather

import kotlin.math.pow

object SeaLevelPressureCalibrator {
    fun calibrate(rawPressure: Float, altitude: Float): Float {
        return rawPressure * (1 - altitude / 44330.0).pow(-5.255).toFloat()
    }
}