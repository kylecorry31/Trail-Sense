package com.kylecorry.trail_sense.tools.metaldetector.ui

import java.time.Duration

class Debouncer(var debounceTime: Duration, initialValue: Boolean = false) {


    val value: Boolean
        get() {
            if (lastValue == lastAttemptedValue || lastAttemptedTime - lastTime > debounceTime.toMillis()) {
                lastValue = lastAttemptedValue
                lastTime = lastAttemptedTime
            }
            return lastValue
        }

    private var lastValue = initialValue
    private var lastTime = System.currentTimeMillis()

    private var lastAttemptedValue = initialValue
    private var lastAttemptedTime = System.currentTimeMillis()

    fun update(newValue: Boolean) {
        lastAttemptedValue = newValue
        lastAttemptedTime = System.currentTimeMillis()
    }

}