package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.kylecorry.andromeda.battery.Battery
import com.kylecorry.andromeda.battery.BatteryHealth
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences

class BatteryDiagnostic(
    private val context: Context,
    lifecycleOwner: LifecycleOwner,
    private val navController: NavController
) :
    IDiagnostic {

    private val battery = Battery(context)
    private val formatService = FormatServiceV2(context)

    init {
        battery.asLiveData().observe(lifecycleOwner) {}
    }

    override fun getIssues(): List<DiagnosticIssue> {
        val issues = mutableListOf<DiagnosticIssue>()
        val prefs = UserPreferences(context)

        if (prefs.isLowPowerModeOn) {
            issues.add(
                DiagnosticIssue(
                    context.getString(R.string.tool_battery_title),
                    context.getString(R.string.pref_low_power_mode_title),
                    IssueSeverity.Warning,
                    IssueMessage(actionTitle = context.getString(R.string.settings)) {
                        navController.navigate(R.id.powerSettingsFragment)
                    }
                )
            )
        }

        if (!Permissions.isIgnoringBatteryOptimizations(context)) {
            issues.add(
                DiagnosticIssue(
                    context.getString(R.string.tool_battery_title),
                    context.getString(R.string.battery_usage_restricted),
                    IssueSeverity.Warning,
                    IssueMessage(actionTitle = context.getString(R.string.settings)) {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            )
        }

        if (battery.health != BatteryHealth.Good && battery.health != BatteryHealth.Unknown) {
            issues.add(
                DiagnosticIssue(
                    context.getString(R.string.tool_battery_title),
                    formatService.formatBatteryHealth(battery.health),
                    IssueSeverity.Error
                )
            )
        }

        return issues
    }
}