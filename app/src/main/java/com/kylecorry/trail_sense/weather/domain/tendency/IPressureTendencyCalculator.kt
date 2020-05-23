package com.kylecorry.trail_sense.weather.domain.tendency

import com.kylecorry.trail_sense.weather.domain.PressureReading
import org.threeten.bp.Duration

interface IPressureTendencyCalculator {

    fun calculate(
        readings: List<PressureReading>,
        duration: Duration = Duration.ofHours(3)
    ): PressureTendency

}