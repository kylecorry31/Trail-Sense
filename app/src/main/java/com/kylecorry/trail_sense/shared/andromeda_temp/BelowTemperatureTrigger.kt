package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.sol.science.ecology.LifecycleEventFactors
import com.kylecorry.sol.science.ecology.triggers.LifecycleEventTrigger
import com.kylecorry.sol.units.Temperature

enum class TemperatureTriggerType {
    High,
    Low,
    Average
}

class BelowTemperatureTrigger(
    threshold: Temperature = Temperature.celsius(0f),
    private val triggerType: TemperatureTriggerType = TemperatureTriggerType.Low
) : LifecycleEventTrigger {

    private val thresholdC = threshold.celsius().value

    override fun isTriggered(factors: LifecycleEventFactors): Boolean {
        val temperature = when (triggerType) {
            TemperatureTriggerType.High -> factors.temperature.current.end
            TemperatureTriggerType.Low -> factors.temperature.current.start
            TemperatureTriggerType.Average -> Temperature.celsius((factors.temperature.current.end.celsius().value - factors.temperature.current.start.celsius().value) / 2f)
        }

        return temperature.celsius().value < thresholdC
    }

}