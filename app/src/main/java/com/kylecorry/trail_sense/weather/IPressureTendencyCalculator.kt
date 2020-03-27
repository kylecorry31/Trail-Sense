package com.kylecorry.trail_sense.weather

import com.kylecorry.trail_sense.models.PressureReading
import com.kylecorry.trail_sense.models.PressureTendency
import java.time.Duration

interface IPressureTendencyCalculator  {
    fun getPressureTendency(readings: List<PressureReading>, duration: Duration = Duration.ofHours(3)): PressureTendency
}