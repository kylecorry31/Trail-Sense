package com.kylecorry.trail_sense.settings.backup

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.IDismissibleAlerter
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils

class BackupFailedAlerter(private val context: Context) : IDismissibleAlerter {
    override fun alert() {
        val intent = NavigationUtils.pendingIntent(context, R.id.diagnosticsFragment)

        val notification = Notify.status(
            context,
            CHANNEL_BACKUP_FAILED,
            context.getString(R.string.backup_failed),
            context.getString(R.string.unable_to_create_an_automatic_backup),
            R.drawable.ic_alert,
            group = NOTIFICATION_GROUP_BACKUP_FAILED,
            intent = intent,
            autoCancel = true,
            alertOnlyOnce = true,
        )
        AppServiceRegistry.get<NotificationSubsystem>().send(NOTIFICATION_ID, notification)
    }

    override fun dismiss() {
        Notify.cancel(context, NOTIFICATION_ID)
    }

    companion object {
        private const val NOTIFICATION_ID = 2739842
        private const val NOTIFICATION_GROUP_BACKUP_FAILED = "trail_sense_backup_failed"
        const val CHANNEL_BACKUP_FAILED = "backup_failed"
    }
}