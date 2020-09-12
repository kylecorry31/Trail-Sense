package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.trailsensecore.infrastructure.system.AlarmUtils
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import com.kylecorry.trail_sense.weather.infrastructure.receivers.WeatherUpdateReceiver

object WeatherAlarmScheduler {
    fun start(context: Context) {
        context.sendBroadcast(WeatherUpdateReceiver.intent(context.applicationContext))
    }

    fun stop(context: Context) {
        val pi = WeatherUpdateReceiver.pendingIntent(context)
        AlarmUtils.cancel(context, pi)

        NotificationUtils.cancel(context, WeatherNotificationService.WEATHER_NOTIFICATION_ID)
    }
}