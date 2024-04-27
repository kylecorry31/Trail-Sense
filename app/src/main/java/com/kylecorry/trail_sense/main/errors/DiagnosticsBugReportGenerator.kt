package com.kylecorry.trail_sense.main.errors

import android.content.Context
import com.kylecorry.andromeda.exceptions.IBugReportGenerator
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.AccelerometerDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.AlarmDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.AltimeterDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.BarometerDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.BatteryDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.CameraDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.FlashlightDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.GPSDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.LightSensorDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.MagnetometerDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.NotificationDiagnostic
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.PedometerDiagnostic

class DiagnosticsBugReportGenerator : IBugReportGenerator {
    override fun generate(context: Context, throwable: Throwable): String {
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