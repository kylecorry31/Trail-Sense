package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.app.Activity
import android.content.Context
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.battery.BatteryToolRegistration
import com.kylecorry.trail_sense.tools.paths.infrastructure.BacktrackScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LowPowerMode(val context: Context) {

    private val prefs by lazy { UserPreferences(context) }
    private val scope = CoroutineScope(Dispatchers.Default)

    fun enable(activity: Activity? = null) {
        prefs.isLowPowerModeOn = true

        context.sendBroadcast(
            Intents.localIntent(
                context,
                BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_ENABLED
            )
        )

        if (prefs.lowPowerModeDisablesBacktrack) {
            BacktrackScheduler.stop(context)
        }

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

        scope.launch {
            if (BacktrackScheduler.isOn(context)) {
                BacktrackScheduler.start(context, false)
            }
        }
    }

    fun isEnabled(): Boolean {
        return prefs.isLowPowerModeOn
    }
}