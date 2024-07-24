package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.light.LightSensor
import com.kylecorry.trail_sense.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class LightSensorDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        return if (!Sensors.hasSensor(context, Sensor.TYPE_LIGHT)) {
            listOf(
                ToolDiagnosticResult(
                    "light-sensor-unavailable",
                    ToolDiagnosticSeverity.Error,
                    context.getString(R.string.tool_light_meter_title),
                    context.getString(R.string.unavailable)
                )
            )
        } else {
            emptyList()
        }
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        val lightSensor = LightSensor(context, SensorManager.SENSOR_DELAY_NORMAL)
        return lightSensor.flow.map {
            val diagnosticResults = mutableListOf<ToolDiagnosticResult>()
            if (lightSensor.quality == Quality.Poor) {
                diagnosticResults.add(
                    ToolDiagnosticResult(
                        "light-sensor-poor-quality",
                        ToolDiagnosticSeverity.Warning,
                        context.getString(R.string.tool_light_meter_title),
                        context.getString(R.string.quality_poor)
                    )
                )
            }
            diagnosticResults.toList()
        }.onStart { emit(emptyList()) }.combine(flowOf(quickScan(context))) { a, b -> a + b }
    }
}