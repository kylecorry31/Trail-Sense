package com.kylecorry.trail_sense.astronomy.infrastructure

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.DeferredTaskScheduler
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.infrastructure.commands.AstronomyAlertCommand
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration
import java.time.LocalTime

class AstronomyDailyWorker(context: Context, params: WorkerParameters) : DailyWorker(
    context,
    params
) {

    override fun isEnabled(context: Context): Boolean {
        val prefs = UserPreferences(context)
        return prefs.astronomy.sendAstronomyAlerts
    }

    override fun getScheduledTime(context: Context): LocalTime {
        val prefs = UserPreferences(context)
        return prefs.astronomy.astronomyAlertTime
    }

    override fun getLastRunKey(context: Context): String {
        return "pref_astronomy_alerts_last_run_date"
    }

    override suspend fun execute(context: Context) {
        AstronomyAlertCommand(context).execute()
    }

    override val uniqueId: String = UNIQUE_ID


    override fun getForegroundInfo(context: Context): ForegroundInfo? {
        val notification = Notify.background(
            context,
            NotificationChannels.CHANNEL_BACKGROUND_UPDATES,
            context.getString(R.string.background_update),
            context.getString(R.string.checking_for_astronomy_events),
            R.drawable.ic_update,
            group = NotificationChannels.GROUP_UPDATES
        )

        val notificationId = 687432

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    companion object {
        private const val UNIQUE_ID = "com.kylecorry.trail_sense.astronomy.AstronomyDailyWorker"
        fun start(context: Context) {
            DeferredTaskScheduler(context, AstronomyDailyWorker::class.java, UNIQUE_ID).schedule(
                Duration.ZERO
            )
        }
    }
}