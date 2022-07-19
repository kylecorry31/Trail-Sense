package com.kylecorry.trail_sense.weather.domain.tendency

import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import java.time.Duration
import kotlin.math.abs

abstract class BasePressureTendencyCalculator: IPressureTendencyCalculator {

    private val changeThreshold = 0.5f

    override fun calculate(
        readings: List<Reading<Pressure>>,
        duration: Duration
    ): PressureTendency {
        val change = getChangeAmount(readings, duration)

        return when {
            abs(change) < changeThreshold -> {
                PressureTendency(
                    PressureCharacteristic.Steady,
                    change
                )
            }
            change < 0 -> {
                PressureTendency(
                    PressureCharacteristic.Falling,
                    change
                )
            }
            else -> {
                PressureTendency(
                    PressureCharacteristic.Rising,
                    change
                )
            }
        }
    }

    protected abstract fun getChangeAmount(readings: List<Reading<Pressure>>, duration: Duration): Float

}