package com.kylecorry.trail_sense.shared.andromeda_temp

class Hysteresis(
    private val threshold: Float,
    private val hysteresisAmount: Float,
    private val trueIfAbove: Boolean = true
) {

    private var wasRun = false
    private var wasTriggered = false

    fun update(value: Float): Boolean {
        val actualThreshold = if (wasRun) {
            if (wasTriggered) {
                threshold - hysteresisAmount
            } else {
                threshold + hysteresisAmount
            }
        } else {
            threshold
        }

        wasRun = true
        wasTriggered = if (trueIfAbove) {
            value >= actualThreshold
        } else {
            value <= actualThreshold
        }
        return wasTriggered
    }

}