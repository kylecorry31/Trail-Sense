package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.system.AlarmUtils
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import com.kylecorry.trail_sense.weather.infrastructure.receivers.WeatherUpdateReceiver
import com.kylecorry.trail_sense.weather.infrastructure.services.WeatherUpdateService

object WeatherUpdateScheduler {
    fun start(context: Context) {
//        if (runInForeground(context)) {
//            WeatherUpdateService.start(context.applicationContext)
//        } else {
//            context.sendBroadcast(WeatherUpdateReceiver.intent(context.applicationContext))
//        }
        val freq = UserPreferences(context).weather.weatherUpdateFrequency
        WeatherUpdateWorker.start(context, freq)
    }

    fun stop(context: Context) {
//        val pi = WeatherUpdateReceiver.pendingIntent(context)
//        AlarmUtils.cancel(context, pi)
//        WeatherUpdateService.stop(context.applicationContext)
        NotificationUtils.cancel(context, WeatherNotificationService.WEATHER_NOTIFICATION_ID)
        WeatherUpdateWorker.stop(context)
    }

    private fun runInForeground(context: Context): Boolean {
        return UserPreferences(context).weather.foregroundService
    }
}