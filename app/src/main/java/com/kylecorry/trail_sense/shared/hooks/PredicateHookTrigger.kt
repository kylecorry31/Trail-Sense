package com.kylecorry.trail_sense.shared.hooks

class PredicateHookTrigger {
    private var lastValue: Boolean? = null
    private val lock = Any()
    private var lastReturnValue = false

    fun getValue(
        behavior: TriggerBehavior = TriggerBehavior.Rising,
        predicate: () -> Boolean
    ): Boolean {
        synchronized(lock) {

            val value = predicate()

            val shouldTrigger = when (behavior) {
                TriggerBehavior.Rising -> {
                    value && (lastValue != true)
                }

                TriggerBehavior.Falling -> {
                    !value && (lastValue != false)
                }

                TriggerBehavior.High -> value
                TriggerBehavior.Low -> !value
                TriggerBehavior.Change -> value != lastValue
            }

            lastValue = value

            if (shouldTrigger) {
                lastReturnValue = !lastReturnValue
                return lastReturnValue
            }

            return lastReturnValue
        }
    }

    enum class TriggerBehavior {
        Rising,
        Falling,
        High,
        Low,
        Change
    }
}