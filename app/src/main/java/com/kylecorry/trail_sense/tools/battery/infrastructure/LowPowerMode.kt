package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.app.Activity
import android.content.Context
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.battery.BatteryToolRegistration

class LowPowerMode(val context: Context) {

    private val prefs by lazy { UserPreferences(context) }

    fun enable(activity: Activity? = null) {
        prefs.isLowPowerModeOn = true

        context.sendBroadcast(
            Intents.localIntent(
                context,
                BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_ENABLED
            )
        )

        activity?.recreate()
    }

    fun disable(activity: Activity? = null) {
        prefs.isLowPowerModeOn = false

        context.sendBroadcast(
            Intents.localIntent(
                context,
                BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_DISABLED
            )
        )

        if (activity != null) {
            activity.recreate()
            return
        }
    }

    fun isEnabled(): Boolean {
        return prefs.isLowPowerModeOn
    }
}