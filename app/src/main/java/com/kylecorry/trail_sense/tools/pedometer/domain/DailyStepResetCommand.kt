package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.luna.specifications.Specification
import com.kylecorry.trail_sense.settings.infrastructure.IPedometerPreferences
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.IsTodaySpecification
import java.time.Instant

class DailyStepResetCommand(
    private val preferences: IPedometerPreferences,
    private val stepTrackerService: IStepTrackerService,
    private val isToday: Specification<Instant> = IsTodaySpecification()
) : CoroutineCommand {
    override suspend fun execute() {
        val openPeriod = stepTrackerService.getOpenStepTrackingPeriod()
        val wasResetToday = openPeriod?.startTime?.let(isToday::isSatisfiedBy) ?: false
        if (!wasResetToday && preferences.resetDaily) {
            stepTrackerService.startNewStepTrackingPeriod()
        }
    }
}
