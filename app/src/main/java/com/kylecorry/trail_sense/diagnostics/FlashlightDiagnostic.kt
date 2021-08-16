package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.trail_sense.R

class FlashlightDiagnostic(private val context: Context) : IDiagnostic {
    override fun getIssues(): List<DiagnosticIssue> {

        val issues = mutableListOf<DiagnosticIssue>()

        if (!Torch.isAvailable(context)) {
            issues.add(
                DiagnosticIssue(
                    context.getString(R.string.flashlight_title),
                    context.getString(R.string.gps_unavailable),
                    IssueSeverity.Error
                )
            )
        }

        return issues
    }
}