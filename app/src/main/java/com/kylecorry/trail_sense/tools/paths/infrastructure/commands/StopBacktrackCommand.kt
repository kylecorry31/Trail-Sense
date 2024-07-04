package com.kylecorry.trail_sense.tools.paths.infrastructure.commands

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class StopBacktrackCommand(private val context: Context) : Command {
    override fun execute() {
        val prefs = UserPreferences(context)
        val wasEnabled = prefs.backtrackEnabled
        prefs.backtrackEnabled = false
        if (wasEnabled) {
            Tools.broadcast(PathsToolRegistration.BROADCAST_BACKTRACK_DISABLED)
        }
        BacktrackScheduler.stop(context)
    }
}