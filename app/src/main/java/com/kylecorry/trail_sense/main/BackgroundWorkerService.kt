package com.kylecorry.trail_sense.main

import android.app.Notification
import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.services.ForegroundService
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R

class BackgroundWorkerService : ForegroundService() {
    override val foregroundNotificationId: Int
        get() = 723824

    override fun getForegroundNotification(): Notification {
        return Notify.background(
            applicationContext,
            NotificationChannels.CHANNEL_BACKGROUND_LAUNCHER,
            getString(R.string.running_in_background),
            null,
            R.drawable.ic_update,
            NotificationChannels.GROUP_UPDATES,
            showForegroundImmediate = true
        )
    }

    override fun onServiceStarted(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        stopService(true)
        super.onDestroy()
    }

    companion object {
        fun intent(context: Context): Intent {
            return Intent(context, BackgroundWorkerService::class.java)
        }

        fun start(context: Context) {
            Intents.startService(context, intent(context), foreground = true)
        }

        fun stop(context: Context) {
            context.stopService(intent(context))
        }

    }
}