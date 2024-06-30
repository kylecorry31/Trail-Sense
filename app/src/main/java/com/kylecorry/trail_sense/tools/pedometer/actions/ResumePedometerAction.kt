package com.kylecorry.trail_sense.tools.pedometer.actions

import android.content.Context
import android.os.Bundle
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Action

class ResumePedometerAction : Action {
    override suspend fun onReceive(context: Context, data: Bundle) {
        if (UserPreferences(context).pedometer.isEnabled) {
            StepCounterService.start(context)
        }
    }
}