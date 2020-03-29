package com.kylecorry.trail_sense.weather.altimeter

import com.kylecorry.trail_sense.shared.AltitudeReading
import com.kylecorry.trail_sense.shared.PressureAltitudeReading

interface IAltitudeCalculator {

    fun convert(readings: List<PressureAltitudeReading>): List<AltitudeReading>

}