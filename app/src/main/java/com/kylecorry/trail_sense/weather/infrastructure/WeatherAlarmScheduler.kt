package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.SystemUtils

object WeatherAlarmScheduler {
    fun start(context: Context) {
        context.sendBroadcast(WeatherUpdateReceiver.intent(context))
    }

    fun stop(context: Context) {
        val pi = WeatherUpdateReceiver.pendingIntent(context)
        SystemUtils.cancelAlarm(context, pi)

        SystemUtils.cancelNotification(context, WeatherNotificationService.WEATHER_NOTIFICATION_ID)
    }
}