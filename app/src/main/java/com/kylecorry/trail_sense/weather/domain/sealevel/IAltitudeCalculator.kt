package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trail_sense.weather.domain.AltitudeReading
import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading

internal interface IAltitudeCalculator {

    fun convert(readings: List<PressureAltitudeReading>): List<AltitudeReading>

}