package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trail_sense.weather.domain.AltitudeReading
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading

internal interface IAltitudeCalculator {

    fun convert(readings: List<PressureAltitudeReading>, interpolateAltitudeChanges: Boolean = false): List<AltitudeReading>

}