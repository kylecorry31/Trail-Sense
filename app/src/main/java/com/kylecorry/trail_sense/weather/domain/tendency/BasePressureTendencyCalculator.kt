package com.kylecorry.trail_sense.weather.domain.tendency

import com.kylecorry.trailsensecore.domain.weather.PressureReading
import java.time.Duration
import kotlin.math.abs

abstract class BasePressureTendencyCalculator: IPressureTendencyCalculator {

    private val changeThreshold = 0.5f

    override fun calculate(
        readings: List<PressureReading>,
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

    protected abstract fun getChangeAmount(readings: List<PressureReading>, duration: Duration): Float

}