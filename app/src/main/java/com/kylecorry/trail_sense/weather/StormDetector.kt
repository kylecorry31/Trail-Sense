package com.kylecorry.trail_sense.weather

import com.kylecorry.trail_sense.models.PressureReading
import java.time.Duration
import java.time.Instant

class StormDetector: IStormDetector {
    override fun isStormIncoming(readings: List<PressureReading>): Boolean {
        val threeHourChange = getRecentChangeAmount(readings)
        return threeHourChange <= -STORM_THRESHOLD
    }

    private fun getRecentChangeAmount(readings: List<PressureReading>): Float {
        val filtered = readings.filter { Duration.between(it.time, Instant.now()) <= STORM_PREDICTION_DURATION }
        if (filtered.size < 2) return 0f
        val firstReading = filtered.first()
        val lastReading = filtered.last()
        return lastReading.value - firstReading.value
    }

    companion object {
        private val STORM_PREDICTION_DURATION = Duration.ofHours(3).plusMinutes(5)
        private const val STORM_THRESHOLD = 6
    }

}