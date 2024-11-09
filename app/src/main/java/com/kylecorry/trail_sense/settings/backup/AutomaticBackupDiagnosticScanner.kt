package com.kylecorry.trail_sense.settings.backup

import android.content.Context
import com.kylecorry.andromeda.files.ContentFileSystem
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticResult
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticSeverity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class AutomaticBackupDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        val prefs = UserPreferences(context)

        if (!prefs.backup.isAutoBackupEnabled) {
            return emptyList()
        }

        val uri = prefs.backup.autoBackupUri ?: return emptyList()
        val contentFileSystem = ContentFileSystem(context, uri)

        if (!contentFileSystem.canWrite()) {
            return listOf(
                ToolDiagnosticResult(
                    "automatic-backup-unwritable",
                    ToolDiagnosticSeverity.Error,
                    context.getString(R.string.automatic_backup),
                    context.getString(R.string.automatic_backup_invalid_directory),
                    context.getString(R.string.automatic_backup_invalid_directory_resolution),
                    ToolDiagnosticAction.navigate(
                        R.id.action_settings,
                        context.getString(R.string.open)
                    )
                )
            )
        }

        return emptyList()
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        return flowOf(quickScan(context))
    }
}