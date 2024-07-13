package com.kylecorry.trail_sense.tools.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticResult
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticSeverity
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class WeatherMonitorDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        if (Sensors.hasBarometer(context)) {
            val isRunning =
                Tools.getService(context, WeatherToolRegistration.SERVICE_WEATHER_MONITOR)
                    ?.isRunning() == true
            if (!isRunning) {
                return listOf(
                    ToolDiagnosticResult(
                        "weather-monitor-disabled",
                        ToolDiagnosticSeverity.Warning,
                        context.getString(R.string.weather),
                        context.getString(R.string.weather_monitoring_disabled),
                        context.getString(R.string.weather_monitor_disabled_resolution),
                        ToolDiagnosticAction.navigate(
                            R.id.weatherSettingsFragment,
                            context.getString(R.string.settings)
                        )
                    )
                )
            }
        }
        return emptyList()
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        return flowOf(quickScan(context))
    }
}