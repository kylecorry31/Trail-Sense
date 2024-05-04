package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import android.os.Build
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.permissions.SpecialPermission
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.diagnostics.domain.Severity
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDiagnosticAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDiagnosticResult
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDiagnosticScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class AlarmDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        return if (!Permissions.hasPermission(context, SpecialPermission.SCHEDULE_EXACT_ALARMS)) {
            listOf(ToolDiagnosticResult(
                "exact-alarm-no-permission",
                Severity.Warning,
                context.getString(R.string.permission_alarms_and_reminders),
                context.getString(R.string.no_permission),
                context.getString(
                    R.string.grant_permission,
                    context.getString(R.string.permission_alarms_and_reminders)
                ),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ToolDiagnosticAction(context.getString(R.string.settings)) {
                        Permissions.requestPermission(
                            context,
                            SpecialPermission.SCHEDULE_EXACT_ALARMS
                        )
                    }
                } else {
                    null
                }
            ))
        } else {
            emptyList()
        }
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        return flowOf(quickScan(context))
    }
}