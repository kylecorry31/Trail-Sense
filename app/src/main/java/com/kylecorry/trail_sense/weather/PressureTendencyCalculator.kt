package com.kylecorry.trail_sense.weather

import com.kylecorry.trail_sense.models.PressureReading
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

object PressureTendencyCalculator {

    private const val CHANGE_THRESHOLD = 0.1f

    fun getPressureTendency(readings: List<PressureReading>): PressureTendency {
        val change = getChangeAmount(readings, Duration.ofHours(3))

        return when {
            abs(change) < CHANGE_THRESHOLD -> {
                PressureTendency(PressureCharacteristic.Steady, change)
            }
            change < 0 -> {
                PressureTendency(PressureCharacteristic.Falling, change)
            }
            else -> {
                PressureTendency(PressureCharacteristic.Rising, change)
            }
        }
    }

    private fun getChangeAmount(readings: List<PressureReading>, duration: Duration): Float {
        val filtered = readings.filter { Duration.between(it.time, Instant.now()) <= duration }
        if (filtered.size < 2) return 0f
        val firstReading = filtered.first()
        val lastReading = filtered.last()
        return lastReading.value - firstReading.value
    }
}