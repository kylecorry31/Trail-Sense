package com.kylecorry.trail_sense.tools.battery.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.Command

class PowerSavingModeAlertCommand(private val context: Context) : Command {

    private val preference = UserPreferences(context)

    override fun execute() {
        if (preference.isLowPowerModeOn) {
            Alerts.toast(context, context.getString(R.string.low_power_mode_on_message))
        }
    }
}