package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.sol.science.ecology.LifecycleEventFactors
import com.kylecorry.sol.science.ecology.triggers.LifecycleEventTrigger

class OffsetLifecycleEventTrigger(
    private val baseTrigger: LifecycleEventTrigger,
    private val days: Long
) : LifecycleEventTrigger {

    private var triggerDays = 0L

    override fun isTriggered(factors: LifecycleEventFactors): Boolean {
        if (!baseTrigger.isTriggered(factors)) {
            triggerDays = 0
            return false
        }

        val isTriggered = triggerDays >= days
        triggerDays++
        return isTriggered
    }
}