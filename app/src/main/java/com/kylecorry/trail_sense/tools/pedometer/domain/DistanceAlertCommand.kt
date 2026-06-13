package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.trail_sense.settings.infrastructure.IPedometerPreferences
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand

class DistanceAlertCommand(
    private val prefs: IPedometerPreferences,
    private val stepTrackerService: IStepTrackerService,
    private val paceCalculator: IPaceCalculator,
    private val alerter: IAlerter
) : CoroutineCommand {
    override suspend fun execute() {
        val alertDistance = prefs.alertDistance ?: return
        val steps = stepTrackerService.getOpenStepTrackingPeriod()?.steps ?: 0L
        val distance = paceCalculator.distance(steps)
        if (distance.meters().value >= alertDistance.meters().value) {
            alerter.alert()
            prefs.alertDistance = null
        }
    }
}
