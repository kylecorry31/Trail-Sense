package com.kylecorry.trail_sense.weather

import com.kylecorry.trail_sense.models.PressureAltitudeReading
import com.kylecorry.trail_sense.models.PressureReading

interface ISeaLevelPressureConverter {

    fun convert(readings: List<PressureAltitudeReading>): List<PressureReading>

}