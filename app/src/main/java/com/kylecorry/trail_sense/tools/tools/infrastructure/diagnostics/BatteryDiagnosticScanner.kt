package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import com.kylecorry.andromeda.battery.Battery
import com.kylecorry.andromeda.battery.BatteryHealth
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.RemoveBatteryRestrictionsCommand
import com.kylecorry.trail_sense.tools.diagnostics.domain.Severity
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDiagnosticAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDiagnosticResult
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDiagnosticScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class BatteryDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        val issues = mutableListOf<ToolDiagnosticResult>()
        val prefs = UserPreferences(context)

        if (prefs.isLowPowerModeOn) {
            issues.add(
                ToolDiagnosticResult(
                    "power-saving-mode",
                    Severity.Warning,
                    context.getString(R.string.pref_low_power_mode_title),
                    context.getString(R.string.on),
                    context.getString(R.string.power_saving_mode_resolution),
                    ToolDiagnosticAction.navigate(
                        R.id.powerSettingsFragment,
                        context.getString(R.string.settings)
                    )
                )
            )
        }

        if (!Permissions.isIgnoringBatteryOptimizations(context)) {
            issues.add(
                ToolDiagnosticResult(
                    "battery-usage-restricted",
                    Severity.Error,
                    context.getString(R.string.tool_battery_title),
                    context.getString(R.string.battery_usage_restricted),
                    context.getString(R.string.battery_restricted_resolution),
                    ToolDiagnosticAction.command(
                        RemoveBatteryRestrictionsCommand(context),
                        context.getString(R.string.settings)
                    )
                )
            )
        }

        return issues
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        val battery = Battery(context)
        return battery.flow.map {
            if (battery.health != BatteryHealth.Good && battery.health != BatteryHealth.Unknown) {
                listOf(
                    ToolDiagnosticResult(
                        "battery-health-poor",
                        Severity.Error,
                        context.getString(R.string.tool_battery_title),
                        context.getString(R.string.quality_poor)
                    )
                )
            } else {
                emptyList()
            }
        }.combine(flowOf(quickScan(context))) { a, b -> a + b }
    }
}
