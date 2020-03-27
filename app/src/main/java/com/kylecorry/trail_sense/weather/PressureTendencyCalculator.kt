package com.kylecorry.trail_sense.weather

import com.kylecorry.trail_sense.models.PressureReading
import com.kylecorry.trail_sense.models.PressureTendency
import java.time.Duration
import java.time.Instant

object PressureTendencyCalculator {

    private const val FAST_CHANGE_THRESHOLD = 2f
    private const val SLOW_CHANGE_THRESHOLD = 0.5f

    fun getPressureTendency(readings: List<PressureReading>, duration: Duration): PressureTendency {
        val change = getChangeAmount(readings, duration)

        return when {
            change <= -FAST_CHANGE_THRESHOLD -> PressureTendency.FALLING_FAST
            change <= -SLOW_CHANGE_THRESHOLD -> PressureTendency.FALLING_SLOW
            change >= FAST_CHANGE_THRESHOLD -> PressureTendency.RISING_FAST
            change >= SLOW_CHANGE_THRESHOLD -> PressureTendency.RISING_SLOW
            else -> PressureTendency.STEADY
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