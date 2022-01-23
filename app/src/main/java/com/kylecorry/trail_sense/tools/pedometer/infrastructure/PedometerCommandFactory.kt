package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.tools.pedometer.domain.DailyStepResetCommand
import com.kylecorry.trail_sense.tools.pedometer.domain.DistanceAlertCommand
import com.kylecorry.trail_sense.tools.pedometer.domain.PedometerService

class PedometerCommandFactory(private val context: Context) {

    private val prefs = UserPreferences(context)
    private val counter = StepCounter(Preferences(context))
    private val pedometerService = PedometerService()


    fun getDistanceAlert(): Command {
        return DistanceAlertCommand(
            prefs.pedometer,
            counter,
            pedometerService,
            DistanceAlertSender(context)
        )
    }

    fun getDailyStepReset(): Command {
        return DailyStepResetCommand(prefs.pedometer, counter)
    }

}