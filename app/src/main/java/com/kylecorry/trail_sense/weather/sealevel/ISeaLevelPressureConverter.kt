package com.kylecorry.trail_sense.weather.sealevel

import com.kylecorry.trail_sense.shared.PressureAltitudeReading
import com.kylecorry.trail_sense.shared.PressureReading

interface ISeaLevelPressureConverter {

    fun convert(readings: List<PressureAltitudeReading>): List<PressureReading>

}