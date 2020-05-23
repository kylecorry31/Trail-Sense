package com.kylecorry.trail_sense.weather.domain.tendency

import com.kylecorry.trail_sense.weather.domain.PressureReading
import org.threeten.bp.Duration
import org.threeten.bp.Instant

class DropPressureTendencyCalculator: BasePressureTendencyCalculator() {

    override fun getChangeAmount(readings: List<PressureReading>, duration: Duration): Float {
        val filtered = readings.filter { Duration.between(it.time, Instant.now()) <= duration }
        if (filtered.size < 2) return 0f
        return filtered.last().value - filtered.first().value
    }
}