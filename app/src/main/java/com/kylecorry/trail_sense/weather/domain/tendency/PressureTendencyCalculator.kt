package com.kylecorry.trail_sense.weather.domain.tendency

import com.kylecorry.trail_sense.shared.PressureReading
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.pow

object PressureTendencyCalculator {

    private const val CHANGE_THRESHOLD = 0.5f

    fun getPressureTendency(
        readings: List<PressureReading>,
        duration: Duration = Duration.ofHours(3)
    ): PressureTendency {
        val change =
            getChangeAmount(
                readings,
                duration
            )

        return when {
            abs(change) < CHANGE_THRESHOLD -> {
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

    private fun getChangeAmount(readings: List<PressureReading>, duration: Duration): Float {
        val filtered = readings.filter { Duration.between(it.time, Instant.now()) <= duration }
        if (filtered.size < 2) return 0f
        return getSlope(
            filtered
        ) * 60 * 60 * 3
    }

    private fun getSlope(readings: List<PressureReading>): Float {

        if (readings.isEmpty()){
            return 0f
        }

        val startTime = readings.first().time.epochSecond
        val xBar = readings.map { it.time.epochSecond - startTime }.average().toFloat()
        val yBar = readings.map { it.value }.average().toFloat()

        var ssxx = 0.0f
        var ssxy = 0.0f
        var ssto = 0.0f

        for (i in readings.indices) {
            val x = (readings[i].time.epochSecond - startTime).toFloat()
            ssxx += (x - xBar).pow(2)
            ssxy += (x - xBar) * (readings[i].value - yBar)
            ssto += (readings[i].value - yBar).pow(2)
        }

        return ssxy / ssxx
    }

}