package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R

class CameraDiagnostic(private val context: Context) : IDiagnostic {
    override fun getIssues(): List<DiagnosticIssue> {

        val issues = mutableListOf<DiagnosticIssue>()

        if (!Permissions.isCameraEnabled(context)) {
            issues.add(
                DiagnosticIssue(
                    context.getString(R.string.camera),
                    context.getString(R.string.no_permission),
                    IssueSeverity.Error,
                    IssueMessage(actionTitle = context.getString(R.string.update)) {
                        val intent = Intents.appSettings(context)
                        context.startActivity(intent)
                    }
                )
            )
        } else if (!Camera.isAvailable(context)) {
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