package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.DeferredTaskScheduler
import com.kylecorry.andromeda.jobs.ITaskScheduler
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.commands.MonitorWeatherCommand
import java.time.LocalDateTime

class WeatherUpdateWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d(javaClass.simpleName, "Started")
        try {
            setForeground(createForegroundInfo(applicationContext))
            MonitorWeatherCommand(applicationContext).execute()
        } catch (e: Exception) {
            throw e
        } finally {
            val frequency = UserPreferences(applicationContext).weather.weatherUpdateFrequency
            scheduler(applicationContext).schedule(frequency)
            Log.d(javaClass.simpleName, "Scheduled next job at ${LocalDateTime.now().plus(frequency)}")

        }
        return Result.success()
    }

    private fun createForegroundInfo(context: Context): ForegroundInfo {
        val notification = Notify.background(
            context,
            NotificationChannels.CHANNEL_BACKGROUND_UPDATES,
            context.getString(R.string.weather_update_notification_channel),
            context.getString(R.string.notification_monitoring_weather),
            R.drawable.ic_update,
            group = NotificationChannels.GROUP_UPDATES
        )

        val notificationId = 629579783

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
        private const val WORK_TAG = "com.kylecorry.trail_sense.WeatherUpdateWorker"

        fun scheduler(context: Context): ITaskScheduler {
            return DeferredTaskScheduler(
                context,
                WeatherUpdateWorker::class.java,
                WORK_TAG
            )
        }
    }

}