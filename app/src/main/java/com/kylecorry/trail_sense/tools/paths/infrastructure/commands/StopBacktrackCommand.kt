package com.kylecorry.trail_sense.tools.paths.infrastructure.commands

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.tools.paths.infrastructure.BacktrackScheduler

class StopBacktrackCommand(private val context: Context): Command {
    override fun execute() {
        UserPreferences(context).backtrackEnabled = false
        BacktrackScheduler.stop(context)
    }
}