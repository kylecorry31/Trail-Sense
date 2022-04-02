package com.kylecorry.trail_sense.navigation.paths.infrastructure.commands

import android.content.Context
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.Command

class StopBacktrackCommand(private val context: Context): Command {
    override fun execute() {
        UserPreferences(context).backtrackEnabled = false
        BacktrackScheduler.stop(context)
    }
}