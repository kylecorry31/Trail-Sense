package com.kylecorry.trail_sense.main

import android.app.Notification
import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.services.ForegroundService
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackIsEnabled
import com.kylecorry.trail_sense.weather.infrastructure.WeatherMonitorIsEnabled

class BackgroundWorkerService : ForegroundService() {
    override val foregroundNotificationId: Int
        get() = 723824

    override fun getForegroundNotification(): Notification {
        return createNotification(applicationContext)
    }

    override fun onServiceStarted(intent: Intent?, flags: Int, startId: Int): Int {
        Notify.send(
            applicationContext,
            foregroundNotificationId,
            createNotification(applicationContext, true)
        )
        return START_STICKY
    }

    override fun onDestroy() {
        stopService(true)
        super.onDestroy()
    }

    // Create notification
    private fun createNotification(context: Context, includeServices: Boolean = false): Notification {
        val contents = if (includeServices){
            getRunningServices().joinToString(" • ").ifEmpty { null }
        } else {
            null
        }

        return Notify.background(
            context,
            NotificationChannels.CHANNEL_BACKGROUND_LAUNCHER,
            getString(R.string.running_in_background),
            contents,
            R.drawable.ic_logo_outline,
            NotificationChannels.GROUP_UPDATES,
            showForegroundImmediate = true
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