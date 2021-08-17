package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.sense.Sensors

class PedometerDiagnostic(private val context: Context) : IDiagnostic {
    override fun scan(): List<DiagnosticCode> {
        if (!Sensors.hasSensor(context, Sensor.TYPE_STEP_COUNTER)) {
            return listOf(DiagnosticCode.PedometerUnavailable)
        }

        if (!Permissions.canRecognizeActivity(context)) {
            return listOf(DiagnosticCode.PedometerNoPermission)
        }

        return emptyList()
    }
}