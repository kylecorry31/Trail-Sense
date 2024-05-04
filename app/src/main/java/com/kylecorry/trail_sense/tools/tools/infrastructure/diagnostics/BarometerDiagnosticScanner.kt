package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import android.hardware.SensorManager
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.barometer.Barometer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.diagnostics.domain.Severity
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDiagnosticResult
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDiagnosticScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class BarometerDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        return if (!Sensors.hasBarometer(context)) {
            listOf(
                ToolDiagnosticResult(
                    "barometer-unavailable",
                    Severity.Error,
                    context.getString(R.string.barometer),
                    context.getString(R.string.unavailable)
                )
            )
        } else {
            emptyList()
        }
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        val barometer = Barometer(context, SensorManager.SENSOR_DELAY_NORMAL)
        return barometer.flow.map {
            if (barometer.quality == Quality.Poor) {
                listOf(
                    ToolDiagnosticResult(
                        "barometer-poor",
                        Severity.Warning,
                        context.getString(R.string.barometer),
                        context.getString(R.string.quality_poor)
                    )
                )
            } else {
                emptyList()
            }
        }.combine(flowOf(quickScan(context))) { a, b -> a + b }
    }
}
