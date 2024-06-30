package com.kylecorry.trail_sense.shared.automations

data class Automation(
    val broadcast: String,
    val receivers: List<AutomationReceiver>
)

data class AutomationReceiver(
    val receiverId: String,
    val enabled: Boolean = true,
)