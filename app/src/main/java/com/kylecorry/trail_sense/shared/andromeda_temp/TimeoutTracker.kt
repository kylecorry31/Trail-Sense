package com.kylecorry.trail_sense.shared.andromeda_temp

class TimeoutTracker(private val timeProvider: TimeProvider = SystemTimeProvider()) {

    private var startMillis: Long? = null

    fun start() {
        startMillis = timeProvider.elapsedRealtime()
    }

    fun reset() {
        startMillis = null
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
