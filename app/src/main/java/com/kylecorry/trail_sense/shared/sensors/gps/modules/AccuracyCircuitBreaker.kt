package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.andromeda.core.time.TimeProvider
import com.kylecorry.trail_sense.shared.andromeda_temp.TimeoutTracker

internal class AccuracyCircuitBreaker(timeProvider: TimeProvider) {
    private val filteringTime = TimeoutTracker(timeProvider)
    private var previousIdWhenTripped: Long? = null
    private var withheldMillis = 0L

    val isOpen: Boolean
        get() = previousIdWhenTripped != null

    /**
     * Trips the breaker if the filter has been rejecting for [timeoutMillis]. [previousId] is the
     * ID of the pipeline's current reading, which is used to detect the next acceptance.
     */
    fun tripIfTimedOut(timeoutMillis: Long, previousId: Long): Boolean {
        if (isOpen) {
            return true
        }
        if (!filteringTime.isTimedOut((timeoutMillis - withheldMillis).coerceAtLeast(0L))) {
            return false
        }
        previousIdWhenTripped = previousId
        return true
    }

    fun resetIfAccepted(previousId: Long) {
        if (isOpen && previousIdWhenTripped != previousId) {
            reset()
        }
    }

    fun reset() {
        previousIdWhenTripped = null
        withheldMillis = 0L
        filteringTime.reset()
    }

    fun pause() {
        withheldMillis += filteringTime.elapsedMillis()
        filteringTime.reset()
    }
}
