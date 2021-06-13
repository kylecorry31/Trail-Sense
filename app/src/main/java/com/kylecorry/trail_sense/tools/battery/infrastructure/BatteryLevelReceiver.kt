package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.shared.LowPowerMode
import com.kylecorry.trail_sense.shared.UserPreferences

class BatteryLevelReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return

        val powerPrefs = UserPreferences(context).power

        val autoBatterySaver = powerPrefs.autoLowPower

        if (!autoBatterySaver){
            return
        }

        if (intent.action == Intent.ACTION_BATTERY_LOW) {
            LowPowerMode(context).enable()
        } else if (intent.action == Intent.ACTION_BATTERY_OKAY && !powerPrefs.userEnabledLowPower){
            LowPowerMode(context).disable()
        }
    }
}