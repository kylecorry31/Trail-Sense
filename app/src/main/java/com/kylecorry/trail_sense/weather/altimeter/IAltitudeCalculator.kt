package com.kylecorry.trail_sense.weather.altimeter

import com.kylecorry.trail_sense.models.AltitudeReading
import com.kylecorry.trail_sense.models.PressureAltitudeReading

interface IAltitudeCalculator {

    fun convert(readings: List<PressureAltitudeReading>): List<AltitudeReading>

}