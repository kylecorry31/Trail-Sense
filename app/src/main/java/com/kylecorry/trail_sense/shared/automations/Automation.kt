package com.kylecorry.trail_sense.shared.automations

import android.os.Bundle

data class Automation(
    val broadcast: String,
    val receivers: List<AutomationReceiver>
)

data class AutomationReceiver(
    val receiverId: String,
    // TODO: Make this more generic / easier to store in the DB
    val parameterTransform: (Bundle) -> Bundle = { it },
)