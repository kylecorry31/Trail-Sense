package com.kylecorry.trail_sense.weather.infrastructure

import android.app.PendingIntent
import android.content.Context
import com.kylecorry.trail_sense.shared.SystemUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration
import java.time.LocalDateTime

object WeatherAlarmScheduler {
    fun start(context: Context){
        val prefs = UserPreferences(context).weather

        if (!prefs.shouldMonitorWeather){
            return
        }

        if (prefs.shouldShowWeatherNotification) {
            val notification = WeatherNotificationService.getDefaultNotification(context)
            SystemUtils.sendNotification(
                context,
                WeatherNotificationService.WEATHER_NOTIFICATION_ID,
                notification
            )
        }
        val intent = WeatherUpdateReceiver.intent(context)
        val pi = PendingIntent.getBroadcast(context, PI_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        SystemUtils.repeatingAlarm(context, LocalDateTime.now(), Duration.ofMinutes(15), pi, false)
    }

    fun stop(context: Context){
        val intent = WeatherUpdateReceiver.intent(context)
        val pi = PendingIntent.getBroadcast(context, PI_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        SystemUtils.cancelAlarm(context, pi)
    }

    private const val PI_ID = 820943
}