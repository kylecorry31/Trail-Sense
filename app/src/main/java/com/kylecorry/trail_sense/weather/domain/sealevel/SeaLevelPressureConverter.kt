package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading
import com.kylecorry.trail_sense.weather.domain.PressureReading
import kotlin.math.pow

internal class SeaLevelPressureConverter() : ISeaLevelPressureConverter {

    override fun convert(readings: List<PressureAltitudeReading>): List<PressureReading> {
        return readings.map {
            PressureReading(
                it.time,
                toSeaLevel(
                    it.pressure,
                    it.altitude
                )
            )
        }
    }

    private fun toSeaLevel(pressure: Float, altitude: Float): Float {
        val t = 30f
        return pressure * (1 - ((0.0065f * altitude) / (t + 0.0065f * altitude + 273.15f))).pow(-5.257f)
    }

}