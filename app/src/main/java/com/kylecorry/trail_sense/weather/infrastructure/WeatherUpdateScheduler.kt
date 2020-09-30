package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.service.WeatherUpdateService
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils

object WeatherUpdateScheduler {
    fun start(context: Context) {
        val freq = UserPreferences(context).weather.weatherUpdateFrequency
        WeatherUpdateWorker.start(context, freq)
    }

    fun stop(context: Context) {
        NotificationUtils.cancel(context, WeatherNotificationService.WEATHER_NOTIFICATION_ID)
        WeatherUpdateWorker.stop(context)
        context.stopService(WeatherUpdateService.intent(context))
    }
}