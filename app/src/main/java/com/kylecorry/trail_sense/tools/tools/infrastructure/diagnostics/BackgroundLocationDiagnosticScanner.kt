package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class BackgroundLocationDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        val sensorService = SensorService(context)

        if (!sensorService.hasLocationPermission(true)) {
            return listOf(
                ToolDiagnosticResult(
                    "background-location-no-permission",
                    ToolDiagnosticSeverity.Warning,
                    context.getString(R.string.gps),
                    context.getString(R.string.no_permission),
                    resolution = context.getString(
                        R.string.grant_permission,
                        context.getString(
                            R.string.background_location_permission
                        )
                    ),
                    action = ToolDiagnosticAction.permissions(context)
                )
            )
        }

        return emptyList()
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        return flowOf(quickScan(context))
    }
}