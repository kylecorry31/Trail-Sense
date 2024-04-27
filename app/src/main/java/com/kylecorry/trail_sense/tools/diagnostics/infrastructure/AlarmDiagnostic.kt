package com.kylecorry.trail_sense.tools.diagnostics.infrastructure

import android.content.Context
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.permissions.SpecialPermission
import com.kylecorry.trail_sense.tools.diagnostics.domain.DiagnosticCode
import com.kylecorry.trail_sense.tools.diagnostics.domain.IDiagnostic

class AlarmDiagnostic(private val context: Context) : IDiagnostic {
    override fun scan(): List<DiagnosticCode> {
        if (!Permissions.hasPermission(context, SpecialPermission.SCHEDULE_EXACT_ALARMS)) {
            return listOf(DiagnosticCode.ExactAlarmNoPermission)
        }

        return emptyList()
    }
}