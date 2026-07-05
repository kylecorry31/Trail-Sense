package com.kylecorry.trail_sense.shared.andromeda_temp

class ThresholdChangeTrigger<T>(
    initialValue: T,
    private val threshold: Float,
    private val distance: (previous: T, current: T) -> Float
) {
    var lastTriggeredValue: T = initialValue
        private set

    fun update(value: T): Boolean {
        if (distance(lastTriggeredValue, value) <= threshold) {
            return false
        }

        lastTriggeredValue = value
        return true
    }

    fun reset(value: T) {
        lastTriggeredValue = value
    }
}
