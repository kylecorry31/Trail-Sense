package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.tools.pedometer.domain.DailyStepResetCommand
import com.kylecorry.trail_sense.tools.pedometer.domain.DistanceAlertCommand
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator

class PedometerCommandFactory(private val context: Context) {

    private val prefs = UserPreferences(context)
    private val stepTrackerService = getAppService<StepTrackerService>()
    private val paceCalculator = StrideLengthPaceCalculator(prefs.pedometer.strideLength)

    fun getDistanceAlert(): CoroutineCommand {
        return DistanceAlertCommand(
            prefs.pedometer,
            stepTrackerService,
            paceCalculator,
            DistanceAlerter(context)
        )
    }

    fun getDailyStepReset(): CoroutineCommand {
        return DailyStepResetCommand(prefs.pedometer, stepTrackerService)
    }

}
