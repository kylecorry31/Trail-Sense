package com.kylecorry.trail_sense.tools.diagnostics.ui

import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.tools.diagnostics.infrastructure.DiagnosticsLogExportService
import java.time.Instant

class DownloadDiagnosticsLogsCommand(private val fragment: AndromedaFragment) {

    fun execute() {
        fragment.inBackground(BackgroundMinimumState.Created) {
            val destination = IntentUriPicker(fragment, fragment.requireContext()).create(
                "trail-sense-logs-${Instant.now().epochSecond}.zip",
                "application/zip"
            ) ?: return@inBackground

            try {
                Alerts.withLoading(
                    fragment.requireContext(),
                    fragment.getString(R.string.exporting)
                ) {
                    DiagnosticsLogExportService(fragment.requireContext()).export(destination)
                }
                fragment.toast(fragment.getString(R.string.exported))
            } catch (_: Exception) {
                fragment.toast(fragment.getString(R.string.export_failed))
            }
        }
    }
}
