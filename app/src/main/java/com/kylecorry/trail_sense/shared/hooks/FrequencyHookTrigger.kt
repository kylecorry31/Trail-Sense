package com.kylecorry.trail_sense.shared.hooks

import java.time.Duration
import java.time.Instant

internal class FrequencyHookTrigger {

    private var lastTime: Instant? = null
    private val lock = Any()
    private var lastReturnValue = false

    fun getValue(time: Instant, threshold: Duration): Boolean {
        synchronized(lock) {
            if (lastTime == null) {
                lastTime = time
                lastReturnValue = !lastReturnValue
                return lastReturnValue
            }

            if (Duration.between(lastTime, time) >= threshold) {
                lastTime = time
                lastReturnValue = !lastReturnValue
                return lastReturnValue
            }

            return lastReturnValue
        }
    }
}