package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.settings.infrastructure.IPedometerPreferences
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.IStepCounter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.IsTodaySpecification
import java.time.Instant

class DailyStepResetCommand(
    private val preferences: IPedometerPreferences,
    private val counter: IStepCounter,
    private val isToday: Specification<Instant> = IsTodaySpecification()
) : Command {
    override fun execute() {
        val wasResetToday = counter.startTime?.let(isToday::isSatisfiedBy) ?: false
        if (!wasResetToday && preferences.resetDaily) {
            counter.reset()
        }
    }
}