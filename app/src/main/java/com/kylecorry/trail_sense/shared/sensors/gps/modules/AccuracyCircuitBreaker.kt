package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.trail_sense.shared.andromeda_temp.TimeProvider
import com.kylecorry.trail_sense.shared.andromeda_temp.TimeoutTracker

/**
 * Controls whether the accuracy filter is allowed to filter readings.
 *
 * While closed, readings can be filtered.
 * It trips once the filter has been rejecting for too long.
 * It stays open until the pipeline accepts a reading.
 */
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

    /**
     * Resets the breaker if the pipeline has accepted a reading since it tripped.
     */
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

    // Keep active filtering time across sessions, but do not count time spent stopped.
    fun pause() {
        withheldMillis += filteringTime.elapsedMillis()
        filteringTime.reset()
    }
}
