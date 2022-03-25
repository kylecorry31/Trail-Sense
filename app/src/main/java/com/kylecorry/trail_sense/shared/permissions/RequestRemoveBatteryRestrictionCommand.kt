package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.Command

class RequestRemoveBatteryRestrictionCommand(private val context: Context) : Command {

    private val preferences = Preferences(context)

    override fun execute() {
        if (IsBatteryUnoptimized().isSatisfiedBy(context)) {
            preferences.putBoolean(SHOWN_KEY, false)
            return
        }

        val isRequired = IsBatteryExemptionRequired().isSatisfiedBy(context)
        if (!isRequired) {
            preferences.putBoolean(SHOWN_KEY, false)
            return
        }

        if (preferences.getBoolean(SHOWN_KEY) == true) {
            return
        }

        preferences.putBoolean(SHOWN_KEY, true)

        Alerts.dialog(
            context,
            context.getString(R.string.remove_battery_restrictions),
            context.getString(R.string.battery_usage_restricted_benefit)
        ) { cancelled ->
            if (!cancelled) {
                RemoveBatteryRestrictionsCommand(context).execute()
            }
        }
    }

    companion object {
        private const val SHOWN_KEY = "cache_battery_exemption_requested"
    }
}