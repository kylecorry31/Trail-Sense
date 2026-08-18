package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ExternalStorageDiagnosticScanner : ToolDiagnosticScanner {
    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        val prefs = getAppService<PreferencesSubsystem>().preferences
        val packageName = prefs.getString(IntentUriPicker.KEY_EXTERNAL_STORAGE_DENIED_BY)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && prefs.getBoolean(IntentUriPicker.KEY_HAS_EXTERNAL_STORAGE_DENIAL) == true && packageName != null) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                setData("package:$packageName".toUri())
            }

            listOf(
                ToolDiagnosticResult(
                    "external-storage-no-permission",
                    ToolDiagnosticSeverity.Warning,
                    context.getString(R.string.external_storage),
                    context.getString(R.string.no_permission),
                    context.getString(R.string.external_storage_diagnostic_instructions),
                    ToolDiagnosticAction(context.getString(R.string.settings)) {
                        // There's no way to verify the file picker's permission directly, so the
                        // denial is cleared when the settings are opened. If it wasn't granted, the
                        // next failed file pick will report it again.
                        prefs.remove(IntentUriPicker.KEY_HAS_EXTERNAL_STORAGE_DENIAL)
                        prefs.remove(IntentUriPicker.KEY_EXTERNAL_STORAGE_DENIED_BY)
                        it.startActivity(intent)
                    }
                )
            )
        } else {
            emptyList()
        }
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        return flowOf(quickScan(context))
    }
}
