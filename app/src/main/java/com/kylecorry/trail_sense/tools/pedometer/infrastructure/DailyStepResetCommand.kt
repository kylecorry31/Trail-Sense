package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.Command
import java.time.LocalDate

// TODO: use interfaces in constructor
class DailyStepResetCommand(
    private val preferences: UserPreferences,
    private val counter: IStepCounter
) : Command {
    override fun execute() {
        val lastReset = counter.startTime
        if (lastReset != null && lastReset.toZonedDateTime().toLocalDate() != LocalDate.now() && preferences.resetOdometerDaily){
            counter.reset()
        }
    }
}