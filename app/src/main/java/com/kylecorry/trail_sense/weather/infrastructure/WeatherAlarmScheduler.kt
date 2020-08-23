package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.system.AlarmUtils
import com.kylecorry.trail_sense.shared.system.NotificationUtils
import com.kylecorry.trail_sense.weather.infrastructure.receivers.WeatherUpdateReceiver

object WeatherAlarmScheduler {
    fun start(context: Context) {
        context.sendBroadcast(WeatherUpdateReceiver.intent(context))
    }

    fun stop(context: Context) {
        val pi = WeatherUpdateReceiver.pendingIntent(context)
        AlarmUtils.cancel(context, pi)

        NotificationUtils.cancel(context, WeatherNotificationService.WEATHER_NOTIFICATION_ID)
    }
}