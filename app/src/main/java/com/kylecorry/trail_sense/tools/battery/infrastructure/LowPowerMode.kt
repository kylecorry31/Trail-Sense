package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.app.Activity
import android.content.Context
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.battery.BatteryToolRegistration
import com.kylecorry.trail_sense.tools.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LowPowerMode(val context: Context) {

    private val prefs by lazy { UserPreferences(context) }
    private val scope = CoroutineScope(Dispatchers.Default)

    fun enable(activity: Activity? = null) {
        prefs.isLowPowerModeOn = true

        context.sendBroadcast(Intents.localIntent(context, BatteryToolRegistration.ACTION_POWER_SAVING_MODE_CHANGED).also {
            it.putExtra(BatteryToolRegistration.PARAM_POWER_SAVING_MODE_ENABLED, true)
        })

        if (prefs.lowPowerModeDisablesBacktrack) {
            BacktrackScheduler.stop(context)
        }

        StepCounterService.stop(context)

        activity?.recreate()
    }

    fun disable(activity: Activity? = null) {
        prefs.isLowPowerModeOn = false

        context.sendBroadcast(Intents.localIntent(context, BatteryToolRegistration.ACTION_POWER_SAVING_MODE_CHANGED).also {
            it.putExtra(BatteryToolRegistration.PARAM_POWER_SAVING_MODE_ENABLED, false)
        })

        if (activity != null){
            activity.recreate()
            return
        }

        scope.launch {
            if (BacktrackScheduler.isOn(context)) {
                BacktrackScheduler.start(context, false)
            }

            if (prefs.pedometer.isEnabled) {
                StepCounterService.start(context)
            }
        }
    }

    fun isEnabled(): Boolean {
        return prefs.isLowPowerModeOn
    }
}