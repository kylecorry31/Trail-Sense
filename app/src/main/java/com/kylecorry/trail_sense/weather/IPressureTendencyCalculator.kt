package com.kylecorry.trail_sense.weather

import com.kylecorry.trail_sense.models.PressureReading

interface IPressureTendencyCalculator  {
    fun getPressureTendency(readings: List<PressureReading>): PressureTendency
}