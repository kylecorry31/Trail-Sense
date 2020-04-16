package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading
import com.kylecorry.trail_sense.weather.domain.PressureReading

interface ISeaLevelPressureConverter {

    fun convert(readings: List<PressureAltitudeReading>): List<PressureReading>

}