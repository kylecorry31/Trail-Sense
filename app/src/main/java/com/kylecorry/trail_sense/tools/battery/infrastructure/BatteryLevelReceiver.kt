package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.logging.Logger

class BatteryLevelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return

        val powerPrefs = UserPreferences(context).power

        val autoBatterySaver = powerPrefs.autoLowPower

        if (!autoBatterySaver) {
            return
        }

        if (intent.action == Intent.ACTION_BATTERY_LOW) {
            getAppService<Logger>().info(TAG, "Battery low, enabling low power mode")
            LowPowerMode(context).enable()
        } else if (intent.action == Intent.ACTION_BATTERY_OKAY && !powerPrefs.userEnabledLowPower) {
            getAppService<Logger>().info(TAG, "Battery okay, disabling low power mode")
            LowPowerMode(context).disable()
        }
    }

    companion object {
        private const val TAG = "BatteryLevelReceiver"
    }
}
