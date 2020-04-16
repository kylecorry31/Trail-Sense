package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trail_sense.weather.domain.AltitudeReading
import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading

internal class GPSAltitudeCalculator :
    IAltitudeCalculator {
    override fun convert(readings: List<PressureAltitudeReading>): List<AltitudeReading> {
        return readings.map {
            AltitudeReading(
                it.time,
                it.altitude
            )
        }
    }
}