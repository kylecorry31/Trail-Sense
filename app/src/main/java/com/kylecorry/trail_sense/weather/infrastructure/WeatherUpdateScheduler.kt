package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.receivers.WeatherUpdateService
import com.kylecorry.trail_sense.weather.infrastructure.services.WeatherUpdateForegroundService
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils

object WeatherUpdateScheduler {
    fun start(context: Context) {
//        if (runInForeground(context)) {
//            WeatherUpdateForegroundService.start(context.applicationContext)
//        } else {
//            // TODO: This no longer works with alarms
//            context.sendBroadcast(WeatherUpdateService.intent(context.applicationContext))
//        }
        val freq = UserPreferences(context).weather.weatherUpdateFrequency
        WeatherUpdateWorker.start(context, freq)
    }

    fun stop(context: Context) {
//        val pi = WeatherUpdateReceiver.pendingIntent(context)
//        AlarmUtils.cancel(context, pi)
//        WeatherUpdateForegroundService.stop(context.applicationContext)
        NotificationUtils.cancel(context, WeatherNotificationService.WEATHER_NOTIFICATION_ID)
        WeatherUpdateWorker.stop(context)
    }

    private fun runInForeground(context: Context): Boolean {
        return UserPreferences(context).weather.foregroundService
    }
}