package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.receivers.WeatherUpdateAlarmReceiver
import com.kylecorry.trail_sense.weather.infrastructure.services.WeatherUpdateService
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import com.kylecorry.trailsensecore.infrastructure.tasks.ITaskScheduler
import java.time.Duration

object WeatherUpdateScheduler {
    fun start(context: Context) {
        val prefs = UserPreferences(context)
        if (prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesWeather){
            return
        }
        val scheduler = getScheduler(context)
        scheduler.schedule(Duration.ZERO)
    }

    fun stop(context: Context) {
        NotificationUtils.cancel(context, WeatherNotificationService.WEATHER_NOTIFICATION_ID)
        val scheduler = getScheduler(context)
        scheduler.cancel()
        context.stopService(WeatherUpdateService.intent(context))
    }

    fun getScheduler(context: Context): ITaskScheduler {
        val prefs = UserPreferences(context)
        return if (prefs.weather.forceWeatherUpdates) {
            WeatherUpdateAlarmReceiver.scheduler(context)
        } else {
            WeatherUpdateWorker.scheduler(context)
        }
    }
}