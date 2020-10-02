package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.weather.infrastructure.services.WeatherUpdateService
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import java.time.Duration

object WeatherUpdateScheduler {
    fun start(context: Context) {
        WeatherUpdateWorker.start(context, Duration.ZERO)
    }

    fun stop(context: Context) {
        NotificationUtils.cancel(context, WeatherNotificationService.WEATHER_NOTIFICATION_ID)
        WeatherUpdateWorker.stop(context)
        context.stopService(WeatherUpdateService.intent(context))
    }
}