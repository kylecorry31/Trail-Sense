package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R

class CameraDiagnostic(private val context: Context) : IDiagnostic {
    override fun getIssues(): List<DiagnosticIssue> {

        val issues = mutableListOf<DiagnosticIssue>()

        if (Permissions.isCameraEnabled(context) && !Camera.isAvailable(context)) {
            issues.add(
                DiagnosticIssue(
                    context.getString(R.string.camera),
                    context.getString(R.string.gps_unavailable),
                    IssueSeverity.Error
                )
            )
        }

        return issues
    }
}