package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import com.kylecorry.andromeda.torch.Torch

class FlashlightDiagnostic(private val context: Context) : IDiagnostic {

    override fun scan(): List<DiagnosticCode> {
        val issues = mutableListOf<DiagnosticCode>()

        if (!Torch.isAvailable(context)) {
            issues.add(DiagnosticCode.FlashlightUnavailable)
        }

        return issues
    }
}