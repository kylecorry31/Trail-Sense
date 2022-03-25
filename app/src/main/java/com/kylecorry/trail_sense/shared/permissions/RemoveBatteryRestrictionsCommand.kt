package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.shared.commands.Command

class RemoveBatteryRestrictionsCommand(private val context: Context): Command {
    override fun execute() {
        context.startActivity(Intents.batteryOptimizationSettings())
    }
}