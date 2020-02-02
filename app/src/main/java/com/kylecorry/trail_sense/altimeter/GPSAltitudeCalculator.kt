package com.kylecorry.trail_sense.altimeter

import com.kylecorry.trail_sense.models.AltitudeReading
import com.kylecorry.trail_sense.models.PressureAltitudeReading

class GPSAltitudeCalculator : IAltitudeCalculator {
    override fun convert(readings: List<PressureAltitudeReading>): List<AltitudeReading> {
        return readings.map { AltitudeReading(it.time, it.altitude) }
    }
}