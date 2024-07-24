package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import android.hardware.SensorManager
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class MagnetometerDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        return if (!SensorService(context).hasCompass()) {
            listOf(
                ToolDiagnosticResult(
                    MAGNETOMETER_UNAVAILABLE,
                    ToolDiagnosticSeverity.Error,
                    context.getString(R.string.pref_compass_sensor_title),
                    context.getString(R.string.unavailable)
                )
            )
        } else {
            emptyList()
        }
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        val magnetometer = Magnetometer(context, SensorManager.SENSOR_DELAY_NORMAL)
        return magnetometer.flow.map {
            if (magnetometer.quality == Quality.Poor) {
                listOf(
                    ToolDiagnosticResult(
                        MAGNETOMETER_POOR,
                        ToolDiagnosticSeverity.Warning,
                        context.getString(R.string.pref_compass_sensor_title),
                        context.getString(R.string.quality_poor),
                        context.getString(
                            R.string.calibrate_compass_dialog_content,
                            context.getString(android.R.string.ok)
                        ),
                        ToolDiagnosticAction.navigate(
                            R.id.calibrateCompassFragment,
                            context.getString(R.string.settings)
                        )
                    )
                )
            } else {
                emptyList()
            }
        }.onStart { emit(emptyList()) }.combine(flowOf(quickScan(context))) { a, b -> a + b }
    }

    companion object {
        const val MAGNETOMETER_UNAVAILABLE = "magnetometer-unavailable"
        const val MAGNETOMETER_POOR = "magnetometer-poor"
    }
}
