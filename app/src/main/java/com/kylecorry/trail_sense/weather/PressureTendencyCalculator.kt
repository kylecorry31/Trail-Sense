package com.kylecorry.trail_sense.weather

import com.kylecorry.trail_sense.models.PressureReading
import com.kylecorry.trail_sense.models.PressureTendency
import java.time.Duration
import java.time.Instant

class PressureTendencyCalculator: IPressureTendencyCalculator {
    override fun getPressureTendency(readings: List<PressureReading>): PressureTendency {
        val change = getRecentChangeAmount(readings)

        return when {
            change <= -FAST_CHANGE_THRESHOLD -> PressureTendency.FALLING_FAST
            change <= -SLOW_CHANGE_THRESHOLD -> PressureTendency.FALLING_SLOW
            change >= FAST_CHANGE_THRESHOLD -> PressureTendency.RISING_FAST
            change >= SLOW_CHANGE_THRESHOLD -> PressureTendency.RISING_SLOW
            else -> PressureTendency.NO_CHANGE
        }
    }

    private fun getRecentChangeAmount(readings: List<PressureReading>): Float {
        val filtered = readings.filter { Duration.between(it.time, Instant.now()) <= CHANGE_DURATION }
        if (filtered.size < 2) return 0f
        val firstReading = filtered.first()
        val lastReading = filtered.last()
        return lastReading.value - firstReading.value
    }

    companion object {
        private const val FAST_CHANGE_THRESHOLD = 2f
        private const val SLOW_CHANGE_THRESHOLD = 0.5f
        private val CHANGE_DURATION = Duration.ofHours(3).plusMinutes(5)
    }

}