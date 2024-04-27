package com.kylecorry.trail_sense.settings.backup

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.andromeda.core.system.CurrentApp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.io.UriPicker

class RestoreCommand(private val context: Context, private val uriPicker: UriPicker) :
    CoroutineCommand {

    private val backupService = BackupService(context)

    override suspend fun execute() {
        // Alert the user before continuing
        val isCancelled = CoroutineAlerts.dialog(
            context,
            context.getString(R.string.restore),
            context.getString(R.string.restore_disclaimer, context.getString(R.string.app_name)),
            okText = context.getString(R.string.restore)
        )

        if (isCancelled) return

        // Select the source file
        val source = uriPicker.open(listOf("application/zip")) ?: return

        try {
            Alerts.withLoading(
                context,
                context.getString(R.string.restoring_loading_message)
            ) {
                backupService.restore(source)
            }

            Alerts.toast(
                context,
                context.getString(R.string.restore_complete_message),
                short = false
            )
            // Restart the app
            CurrentApp.restart(context)
        } catch (e: BackupService.InvalidBackupException) {
            Alerts.toast(context, context.getString(R.string.invalid_backup_file))
        } catch (e: BackupService.NewerBackupException){
            Alerts.toast(context,
                context.getString(
                    R.string.restore_failure_newer_version,
                    context.getString(R.string.app_name)
                ))
        }
    }
}