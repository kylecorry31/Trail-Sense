package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.accelerometer.Accelerometer
import com.kylecorry.trail_sense.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class AccelerometerDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        return if (!Sensors.hasSensor(context, Sensor.TYPE_ACCELEROMETER)) {
            listOf(
                ToolDiagnosticResult(
                    "accelerometer-unavailable",
                    ToolDiagnosticSeverity.Error,
                    context.getString(R.string.gravity),
                    context.getString(R.string.unavailable)
                )
            )
        } else {
            emptyList()
        }
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        val accelerometer = Accelerometer(context, SensorManager.SENSOR_DELAY_NORMAL)
        return accelerometer.flow.map {
            if (accelerometer.quality == Quality.Poor) {
                listOf(
                    ToolDiagnosticResult(
                        "accelerometer-poor",
                        ToolDiagnosticSeverity.Warning,
                        context.getString(R.string.gravity),
                        context.getString(R.string.quality_poor)
                    )
                )
            } else {
                emptyList()
            }
        }.onStart { emit(emptyList()) }.combine(flowOf(quickScan(context))) { a, b -> a + b }
    }
}