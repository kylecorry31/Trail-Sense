package com.kylecorry.trail_sense.weather.domain.tendency

import com.kylecorry.trailsensecore.domain.weather.PressureReading
import com.kylecorry.trailsensecore.domain.weather.PressureTendency
import java.time.Duration

interface IPressureTendencyCalculator {

    fun calculate(
        readings: List<PressureReading>,
        duration: Duration = Duration.ofHours(3)
    ): PressureTendency

}