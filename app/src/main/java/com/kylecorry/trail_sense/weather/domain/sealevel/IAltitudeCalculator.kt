package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trail_sense.shared.AltitudeReading
import com.kylecorry.trail_sense.shared.PressureAltitudeReading

internal interface IAltitudeCalculator {

    fun convert(readings: List<PressureAltitudeReading>): List<AltitudeReading>

}