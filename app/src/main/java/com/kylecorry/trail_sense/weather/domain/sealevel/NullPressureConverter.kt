package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.domain.weather.PressureReading

internal class NullPressureConverter :
    ISeaLevelPressureConverter {
    override fun convert(readings: List<PressureAltitudeReading>, interpolateAltitudeChanges: Boolean): List<PressureReading> {
        return readings.map {
            PressureReading(
                it.time,
                it.pressure
            )
        }
    }

}