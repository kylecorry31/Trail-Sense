package com.kylecorry.trail_sense.weather.domain.sealevel

import kotlin.math.pow

internal object SeaLevelPressureCalibrator {
    fun calibrate(rawPressure: Float, altitude: Float): Float {
        return rawPressure * (1 - altitude / 44330.0).pow(-5.255).toFloat()
    }

    fun calibrate(rawPressure: Float, altitude: Float, temperatureC: Float): Float {
        return rawPressure * (1 - ((0.0065f * altitude) / (temperatureC + 0.0065f * altitude + 273.15f))).pow(
            -5.257f
        )
    }
}