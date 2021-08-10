package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.services.WeatherUpdateService
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
        val notify = Notify(context)
        notify.cancel(WeatherNotificationService.WEATHER_NOTIFICATION_ID)
        val scheduler = getScheduler(context)
        scheduler.cancel()
        context.stopService(WeatherUpdateService.intent(context))
    }

    fun getScheduler(context: Context): ITaskScheduler {
        return WeatherUpdateWorker.scheduler(context)
    }
}