package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FlashlightDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        return if (!FlashlightSubsystem.getInstance(context).isAvailable()) {
            listOf(
                ToolDiagnosticResult(
                    "flashlight-unavailable",
                    ToolDiagnosticSeverity.Warning,
                    context.getString(R.string.flashlight_title),
                    context.getString(R.string.unavailable)
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
