package com.kylecorry.trail_sense.weather.domain.sealevel.dwell

import com.kylecorry.trail_sense.weather.domain.AltitudeReading
import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading

internal interface IAltitudeCalculator {

    fun convert(readings: List<PressureAltitudeReading>, interpolateAltitudeChanges: Boolean = false): List<AltitudeReading>

}