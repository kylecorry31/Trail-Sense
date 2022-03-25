package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.trail_sense.main.LaunchFromBackgroundService
import com.kylecorry.trail_sense.shared.commands.Command

class AllowForegroundWorkersCommand2(private val context: Context) : Command {

    override fun execute() {
        if (AreForegroundWorkersAllowed().isSatisfiedBy(context)) {
            LaunchFromBackgroundService.stop(context)
            return
        }

        if (IsBatteryExemptionRequired().isSatisfiedBy(context)) {
            LaunchFromBackgroundService.start(context)
        } else {
            LaunchFromBackgroundService.stop(context)
        }
    }
}