package com.kylecorry.trail_sense.main

import android.app.Notification
import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.services.ForegroundService
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackIsEnabled
import com.kylecorry.trail_sense.receivers.StopAllReceiver
import com.kylecorry.trail_sense.weather.infrastructure.WeatherMonitorIsEnabled

class BackgroundWorkerService : ForegroundService() {
    override val foregroundNotificationId: Int
        get() = NOTIFICATION_ID

    override fun getForegroundNotification(): Notification {
        return createNotification(applicationContext)
    }

    private val timer = Timer {
        val contents = getRunningServices().joinToString(" • ").ifEmpty { null }
        if (contents != null) {
            Notify.send(
                applicationContext,
                foregroundNotificationId,
                createNotification(this, contents)
            )
        } else {
            stopSelf()
        }
    }

    override fun onServiceStarted(intent: Intent?, flags: Int, startId: Int): Int {
        timer.once(1000)
        return START_STICKY
    }

    override fun onDestroy() {
        timer.stop()
        stopService(true)
        Notify.cancel(this, NOTIFICATION_ID)
        super.onDestroy()
    }

    // Create notification
    private fun createNotification(context: Context, contents: String? = null): Notification {
        return Notify.background(
            context,
            NotificationChannels.CHANNEL_BACKGROUND_LAUNCHER,
            getString(R.string.running_in_background),
            contents,
            R.drawable.ic_logo_monochrome,
            NotificationChannels.GROUP_UPDATES,
            showForegroundImmediate = true,
            intent = MainActivity.pendingIntent(context),
            actions = listOf(
                Notify.action(
                    getString(R.string.stop),
                    StopAllReceiver.pendingIntent(context),
                    R.drawable.ic_cancel
                )
            )
        )
    }

    private fun getRunningServices(): List<String> {
        val services = listOf(
            WeatherMonitorIsEnabled() to getString(R.string.weather_monitor),
            BacktrackIsEnabled() to getString(R.string.backtrack)
        )

        return services.filter { it.first.isSatisfiedBy(applicationContext) }.map { it.second }
    }

    companion object {
        private const val NOTIFICATION_ID = 723824

        fun intent(context: Context): Intent {
            return Intent(context, BackgroundWorkerService::class.java)
        }

        fun start(context: Context) {
            Intents.startService(context, intent(context), foreground = true)
        }

        fun stop(context: Context) {
            context.stopService(intent(context))
            Notify.cancel(context, NOTIFICATION_ID)
        }

    }
}