package com.kylecorry.trail_sense.settings.backup

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.io.UriPicker
import java.time.Instant

class BackupCommand(private val context: Context, private val uriPicker: UriPicker) :
    CoroutineCommand {

    private val backupService = BackupService(context)

    override suspend fun execute() {
        // Alert the user before continuing
        val isCancelled = CoroutineAlerts.dialog(
            context,
            context.getString(R.string.backup),
            context.getString(R.string.backup_disclaimer),
            okText = context.getString(R.string.backup)
        )

        if (isCancelled) return

        // Select the destination file
        val destination = uriPicker.create(
            "trail-sense-${Instant.now().epochSecond}.zip",
            "application/zip"
        ) ?: return

        Alerts.withLoading(
            context,
            context.getString(R.string.backing_up_loading_message)
        ) {
            backupService.backup(destination)
        }

        Alerts.toast(context, context.getString(R.string.done))
    }
}