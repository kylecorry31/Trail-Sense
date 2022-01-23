package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.settings.infrastructure.IPedometerPreferences
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.IStepCounter
import java.time.LocalDate

class DailyStepResetCommand(
    private val preferences: IPedometerPreferences,
    private val counter: IStepCounter
) : Command {
    override fun execute() {
        val lastReset = counter.startTime?.toZonedDateTime()?.toLocalDate()
        if (lastReset != LocalDate.now() && preferences.resetDaily) {
            counter.reset()
        }
    }
}