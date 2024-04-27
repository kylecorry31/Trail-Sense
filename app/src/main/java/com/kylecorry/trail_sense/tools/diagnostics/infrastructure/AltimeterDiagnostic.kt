package com.kylecorry.trail_sense.tools.diagnostics.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.diagnostics.domain.DiagnosticCode
import com.kylecorry.trail_sense.tools.diagnostics.domain.IDiagnostic

class AltimeterDiagnostic(private val context: Context) :
    IDiagnostic {

    override fun scan(): List<DiagnosticCode> {
        val prefs = UserPreferences(context)
        if (prefs.altimeterMode == UserPreferences.AltimeterMode.Override) {
            return listOf(DiagnosticCode.AltitudeOverridden)
        }

        return emptyList()
    }
}