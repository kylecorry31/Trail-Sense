package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.SystemUtils
import com.kylecorry.trail_sense.shared.UserPreferences

object WeatherAlarmScheduler {
    fun start(context: Context) {
        val prefs = UserPreferences(context).weather

        if (!prefs.shouldMonitorWeather) {
            return
        }

        context.sendBroadcast(WeatherUpdateReceiver.intent(context))
    }

    fun stop(context: Context) {
        val pi = WeatherUpdateReceiver.pendingIntent(context)
        SystemUtils.cancelAlarm(context, pi)

        SystemUtils.cancelNotification(context, WeatherNotificationService.WEATHER_NOTIFICATION_ID)
    }
}