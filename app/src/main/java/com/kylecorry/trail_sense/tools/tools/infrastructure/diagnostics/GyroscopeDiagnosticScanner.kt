package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import android.hardware.SensorManager
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.orientation.Gyroscope
import com.kylecorry.trail_sense.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class GyroscopeDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        return if (!Sensors.hasGyroscope(context)) {
            listOf(
                ToolDiagnosticResult(
                    "gyroscope-unavailable",
                    ToolDiagnosticSeverity.Warning,
                    context.getString(R.string.sensor_gyroscope),
                    context.getString(R.string.unavailable)
                )
            )
        } else {
            emptyList()
        }
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        val gyroscope = Gyroscope(context, SensorManager.SENSOR_DELAY_NORMAL)
        return gyroscope.flow.map {
            if (gyroscope.quality == Quality.Poor) {
                listOf(
                    ToolDiagnosticResult(
                        "gyroscope-poor",
                        ToolDiagnosticSeverity.Warning,
                        context.getString(R.string.sensor_gyroscope),
                        context.getString(R.string.quality_poor)
                    )
                )
            } else {
                emptyList()
            }
        }.onStart { emit(emptyList()) }.combine(flowOf(quickScan(context))) { a, b -> a + b }
    }
}
