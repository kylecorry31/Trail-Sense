package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.diagnostics.DiagnosticAlertService
import com.kylecorry.trail_sense.diagnostics.DiagnosticCode
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.navigation.NullAppNavigation

class AllowForegroundWorkersCommand(private val context: Context) : Command {

    private val preferences = Preferences(context)
    private val alerter = DiagnosticAlertService(context, NullAppNavigation())

    override fun execute() {
        if (Permissions.isIgnoringBatteryOptimizations(context)) {
            preferences.putBoolean(SHOWN_KEY, false)
            return
        }

        val isRequired = IsBatteryExemptionRequired().isSatisfiedBy(context)
        if (!isRequired) {
            preferences.putBoolean(SHOWN_KEY, false)
            return
        }

        if (preferences.getBoolean(SHOWN_KEY) == true){
            return
        }

        preferences.putBoolean(SHOWN_KEY, true)
        alerter.alert(DiagnosticCode.BatteryUsageRestricted)
    }

    companion object {
        private const val SHOWN_KEY = "cache_battery_exemption_requested"
    }

}