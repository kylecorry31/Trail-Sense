package com.kylecorry.trail_sense.weather.domain

import java.time.Instant
import kotlin.math.pow

data class PressureAltitudeReading(val time: Instant, val pressure: Float, val altitude: Float, val temperature: Float, val altitudeError: Float? = null){
    fun seaLevel(useTemperature: Boolean = true): PressureReading {
        val pressure = if (useTemperature) {
            pressure * (1 - ((0.0065f * altitude) / (temperature + 0.0065f * altitude + 273.15f))).pow(
                -5.257f
            )
        } else {
            pressure * (1 - altitude / 44330.0).pow(-5.255).toFloat()
        }
        return PressureReading(time, pressure)
    }

    fun pressureReading(): PressureReading {
        return PressureReading(time, pressure)
    }
}