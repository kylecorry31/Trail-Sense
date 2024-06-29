package com.kylecorry.trail_sense.tools.pedometer.receivers

import android.content.Context
import android.os.Bundle
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Receiver

// TODO: Add support for enable/disable pedometer
class SetPedometerStateReceiver : Receiver {
    override fun onReceive(context: Context, data: Bundle) {
        val desiredState = data.getBoolean(PARAM_PEDOMETER_STATE, false)
        val prefs = UserPreferences(context)

        if (desiredState && prefs.pedometer.isEnabled) {
            StepCounterService.start(context)
        } else if (!desiredState) {
            StepCounterService.stop(context)
        }
    }

    companion object {
        const val PARAM_PEDOMETER_STATE = "state"
    }
}