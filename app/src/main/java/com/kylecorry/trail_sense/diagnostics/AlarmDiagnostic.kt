package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import com.kylecorry.andromeda.permissions.Permissions

class AlarmDiagnostic(private val context: Context) : IDiagnostic {
    override fun scan(): List<DiagnosticCode> {
        if (!Permissions.canScheduleExactAlarms(context)) {
            return listOf(DiagnosticCode.ExactAlarmNoPermission)
        }

        return emptyList()
    }
}