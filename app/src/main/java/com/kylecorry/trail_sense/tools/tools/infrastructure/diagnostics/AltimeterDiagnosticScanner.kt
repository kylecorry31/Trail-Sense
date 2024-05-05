package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class AltimeterDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        val prefs = UserPreferences(context)

        return if (prefs.altimeterMode == UserPreferences.AltimeterMode.Override) {
            listOf(
                ToolDiagnosticResult(
                    "altitude-overridden",
                    ToolDiagnosticSeverity.Warning,
                    context.getString(R.string.elevation),
                    context.getString(R.string.overridden),
                    resolution = context.getString(R.string.elevation_override_resolution),
                    action = ToolDiagnosticAction.navigate(
                        R.id.calibrateAltimeterFragment,
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
