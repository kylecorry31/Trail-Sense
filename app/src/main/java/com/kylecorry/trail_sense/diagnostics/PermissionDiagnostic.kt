package com.kylecorry.trail_sense.diagnostics

import android.content.Context
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R

class PermissionDiagnostic(
    private val context: Context,
    private val permission: String,
    private val title: String
) : IDiagnostic {
    override fun getIssues(): List<DiagnosticIssue> {
        val granted = Permissions.hasPermission(context, permission)
        if (!granted) {
            return listOf(
                DiagnosticIssue(
                    title,
                    context.getString(R.string.no_permission),
                    IssueSeverity.Error,
                    IssueMessage(actionTitle = context.getString(R.string.settings)) {
                        val intent = Intents.appSettings(context)
                        context.startActivity(intent)
                    }
                )
            )
        }

        return listOf()
    }
}