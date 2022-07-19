package com.kylecorry.trail_sense.weather.domain.tendency

import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import java.time.Duration

interface IPressureTendencyCalculator {

    fun calculate(
        readings: List<Reading<Pressure>>,
        duration: Duration = Duration.ofHours(3)
    ): PressureTendency

}