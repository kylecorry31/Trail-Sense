package com.kylecorry.trail_sense.shared.errors

import android.content.Context
import com.kylecorry.trail_sense.diagnostics.*

class DiagnosticsBugReportGenerator(private val context: Context) : IBugReportGenerator {
    override fun generate(): String {
        val diagnostics = listOf(
            AccelerometerDiagnostic(context, null),
            MagnetometerDiagnostic(context, null),
            GPSDiagnostic(context, null),
            BarometerDiagnostic(context, null),
            AltimeterDiagnostic(context),
            BatteryDiagnostic(context, null),
            LightSensorDiagnostic(context, null),
            CameraDiagnostic(context),
            FlashlightDiagnostic(context),
            PedometerDiagnostic(context),
            NotificationDiagnostic(context),
            AlarmDiagnostic(context)
        )

        val codes = diagnostics.flatMap { it.scan() }.toSet().sortedBy { it.ordinal }

        return "Diagnostics: ${codes.joinToString(", ") { it.name }}"
    }
}