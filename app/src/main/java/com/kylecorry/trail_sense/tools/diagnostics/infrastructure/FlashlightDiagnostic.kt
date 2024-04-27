package com.kylecorry.trail_sense.tools.diagnostics.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.tools.diagnostics.domain.DiagnosticCode
import com.kylecorry.trail_sense.tools.diagnostics.domain.IDiagnostic
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem

class FlashlightDiagnostic(private val context: Context) : IDiagnostic {

    override fun scan(): List<DiagnosticCode> {
        val issues = mutableListOf<DiagnosticCode>()

        if (!FlashlightSubsystem.getInstance(context).isAvailable()) {
            issues.add(DiagnosticCode.FlashlightUnavailable)
        }

        return issues
    }
}