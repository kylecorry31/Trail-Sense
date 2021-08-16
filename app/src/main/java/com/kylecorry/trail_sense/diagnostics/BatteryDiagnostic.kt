package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.navigation.NavController
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences

class BatteryDiagnostic(private val context: Context, private val navController: NavController) :
    IDiagnostic {
    override fun getIssues(): List<DiagnosticIssue> {
        val issues = mutableListOf<DiagnosticIssue>()
        val prefs = UserPreferences(context)

        if (prefs.isLowPowerModeOn) {
            issues.add(
                DiagnosticIssue(
                    context.getString(R.string.tool_battery_title),
                    context.getString(R.string.pref_low_power_mode_title),
                    IssueSeverity.Warning,
                    IssueMessage(actionTitle = context.getString(R.string.update)) {
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
                    IssueMessage(actionTitle = context.getString(R.string.update)) {
                        val intent = batteryOptimizationSettings()
                        context.startActivity(intent)
                    }
                )
            )
        }

        return issues
    }

    fun batteryOptimizationSettings(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }
}