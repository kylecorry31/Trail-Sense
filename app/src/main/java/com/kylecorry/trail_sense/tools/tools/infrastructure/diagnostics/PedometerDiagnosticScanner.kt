package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class PedometerDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        val results = mutableListOf<ToolDiagnosticResult>()

        if (!Sensors.hasSensor(context, Sensor.TYPE_STEP_COUNTER)) {
            results.add(
                ToolDiagnosticResult(
                    "pedometer-unavailable",
                    ToolDiagnosticSeverity.Error,
                    context.getString(R.string.pedometer),
                    context.getString(R.string.unavailable)
                )
            )
        }

        if (!Permissions.canRecognizeActivity(context)) {
            results.add(
                ToolDiagnosticResult(
                    "pedometer-no-permission",
                    ToolDiagnosticSeverity.Warning,
                    context.getString(R.string.pedometer),
                    context.getString(R.string.no_permission),
                    context.getString(
                        R.string.grant_permission,
                        context.getString(R.string.activity_recognition)
                    ),
                    ToolDiagnosticAction.permissions(context)
                )
            )
        }

        return results
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        return flowOf(quickScan(context))
    }
}
