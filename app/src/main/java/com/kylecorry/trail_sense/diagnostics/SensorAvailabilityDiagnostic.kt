package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R

class SensorAvailabilityDiagnostic(
    private val context: Context,
    private val sensor: Int,
    private val title: String
) : IDiagnostic {
    override fun getIssues(): List<DiagnosticIssue> {
        val exists = Sensors.hasSensor(context, sensor)

        if (!exists) {
            return listOf(
                DiagnosticIssue(
                    title,
                    context.getString(R.string.gps_unavailable),
                    IssueSeverity.Error
                )
            )
        }

        return listOf()
    }
}