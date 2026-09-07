package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.trail_sense.shared.andromeda_temp.TimeProvider
import com.kylecorry.trail_sense.shared.andromeda_temp.TimeoutTracker
import java.time.Instant

internal class GPSRejectionTracker(timeProvider: TimeProvider) {
    private val rejectionTimeout = TimeoutTracker(timeProvider)
    private var pendingFixTime: Instant? = null
    private var withheldMillis = 0L

    fun reset() {
        pendingFixTime = null
        withheldMillis = 0L
        rejectionTimeout.reset()
    }

    // Keep active rejection time across sessions, but do not count time spent stopped.
    fun pause() {
        withheldMillis += rejectionTimeout.elapsedMillis()
        rejectionTimeout.reset()
    }

    fun isAwaitingAcceptance(previousFixTime: Instant): Boolean {
        val pending = pendingFixTime ?: return false
        if (previousFixTime < pending) {
            return true
        }
        reset()
        return false
    }

    fun isTimedOut(timeoutMillis: Long, newFixTime: Instant): Boolean {
        if (!rejectionTimeout.isTimedOut((timeoutMillis - withheldMillis).coerceAtLeast(0L))) {
            return false
        }
        // Later candidates must not move the timestamp we use to acknowledge acceptance.
        if (pendingFixTime == null) {
            pendingFixTime = newFixTime
        }
        return true
    }
}
