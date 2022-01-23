package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import java.time.Instant

class StepCounter(private val preferences: Preferences) : IStepCounter {

    override val startTime: Instant?
        get() = preferences.getInstant(LAST_RESET_KEY)


    override val steps: Long
        get() = preferences.getLong(STEPS_KEY) ?: 0L

    override fun addSteps(steps: Long){
        synchronized(this){
            val lastSteps = this.steps
            preferences.putLong(STEPS_KEY, steps + lastSteps)
            if (startTime == null){
                preferences.putInstant(LAST_RESET_KEY, Instant.now())
            }
        }
    }

    override fun reset(){
        synchronized(this){
            preferences.remove(STEPS_KEY)
            preferences.putInstant(LAST_RESET_KEY, Instant.now())
        }
    }


    companion object {
        const val STEPS_KEY = "cache_steps"
        private const val LAST_RESET_KEY = "last_odometer_reset"
    }

}