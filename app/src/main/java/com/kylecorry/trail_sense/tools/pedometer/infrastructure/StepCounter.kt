package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import com.kylecorry.andromeda.preferences.IPreferences
import java.time.Instant

internal class StepCounter(private val preferences: IPreferences) {

    val startTime: Instant?
        get() = preferences.getInstant(LAST_RESET_KEY)


    val steps: Long
        get() = preferences.getLong(STEPS_KEY) ?: 0L

    fun addSteps(steps: Long) {
        synchronized(this) {
            val lastSteps = this.steps
            preferences.putLong(STEPS_KEY, steps + lastSteps)
            if (startTime == null) {
                preferences.putInstant(LAST_RESET_KEY, Instant.now())
            }
        }
    }

    fun reset() {
        synchronized(this) {
            preferences.remove(STEPS_KEY)
            preferences.putInstant(LAST_RESET_KEY, Instant.now())
        }
    }


    companion object {
        const val STEPS_KEY = "cache_steps"
        const val LAST_RESET_KEY = "last_odometer_reset"
    }

}
