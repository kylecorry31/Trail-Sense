package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.andromeda.core.time.SystemTimeProvider
import com.kylecorry.andromeda.core.time.TimeProvider

class TimeoutTracker(private val timeProvider: TimeProvider = SystemTimeProvider()) {

    private var startMillis: Long? = null

    fun start() {
        startMillis = timeProvider.elapsedRealtime()
    }

    fun reset() {
        startMillis = null
    }

    fun elapsedMillis(): Long {
        return startMillis?.let { timeProvider.elapsedRealtime() - it } ?: 0L
    }

    fun isTimedOut(timeoutMillis: Long, startIfNotStarted: Boolean = true): Boolean {
        val start = startMillis ?: if (startIfNotStarted) {
            start()
            startMillis!!
        } else {
            return false
        }
        val elapsedMillis = timeProvider.elapsedRealtime() - start
        return elapsedMillis >= timeoutMillis
    }
}
