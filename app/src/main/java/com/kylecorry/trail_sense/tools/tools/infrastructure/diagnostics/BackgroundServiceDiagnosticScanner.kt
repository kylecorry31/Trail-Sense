package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.permissions.RemoveBatteryRestrictionsCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class BackgroundServiceDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        return if (!Permissions.isIgnoringBatteryOptimizations(context)) {
            listOf(
                ToolDiagnosticResult(
                    "battery-usage-restricted",
                    ToolDiagnosticSeverity.Error,
                    context.getString(R.string.tool_battery_title),
                    context.getString(R.string.battery_usage_restricted),
                    context.getString(R.string.battery_restricted_resolution),
                    ToolDiagnosticAction.command(
                        RemoveBatteryRestrictionsCommand(context),
                        context.getString(R.string.settings)
                    )
                )
            )
        } else {
            emptyList()
        }
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        return flowOf(quickScan(context))
    }
}
