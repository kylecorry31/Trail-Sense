package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class PowerSaverDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        val prefs = UserPreferences(context)

        return if (prefs.isLowPowerModeOn) {
            listOf(
                ToolDiagnosticResult(
                    "power-saving-mode",
                    ToolDiagnosticSeverity.Warning,
                    context.getString(R.string.pref_low_power_mode_title),
                    context.getString(R.string.on),
                    context.getString(R.string.power_saving_mode_resolution),
                    ToolDiagnosticAction.navigate(
                        R.id.powerSettingsFragment,
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
