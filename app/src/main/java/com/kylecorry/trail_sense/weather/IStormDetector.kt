package com.kylecorry.trail_sense.weather

import com.kylecorry.trail_sense.models.PressureReading

interface IStormDetector {
    fun isStormIncoming(readings: List<PressureReading>): Boolean
}