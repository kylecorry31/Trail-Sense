package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trail_sense.shared.AltitudeReading
import com.kylecorry.trail_sense.shared.PressureAltitudeReading

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