package com.kylecorry.trail_sense.receivers

import android.content.Context
import com.kylecorry.trail_sense.shared.commands.Command

class RestartServicesCommand(private val context: Context): Command {
    override fun execute() {
        TrailSenseServiceUtils.restartServices(context)
    }
}