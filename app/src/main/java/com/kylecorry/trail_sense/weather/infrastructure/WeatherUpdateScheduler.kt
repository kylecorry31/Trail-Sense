package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.jobs.IOneTimeTaskScheduler
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.AllowForegroundWorkersCommand
import java.time.Duration

object WeatherUpdateScheduler {

    fun restart(context: Context) {
        if (WeatherMonitorIsEnabled().isSatisfiedBy(context)) {
            stop(context)
            start(context)
        }
    }

    fun start(context: Context) {
        if (!WeatherMonitorIsAvailable().isSatisfiedBy(context)) {
            return
        }

        AllowForegroundWorkersCommand(context).execute()

        val scheduler = getScheduler(context)
        val prefs = UserPreferences(context)

        if (prefs.weather.weatherUpdateFrequency >= Duration.ofMinutes(15)) {
            WeatherMonitorAlwaysOnService.stop(context)
            scheduler.once()
        } else {
            WeatherMonitorAlwaysOnService.start(context)
            scheduler.cancel()
        }
    }

    fun stop(context: Context) {
        WeatherMonitorAlwaysOnService.stop(context)
        Notify.cancel(context, WEATHER_NOTIFICATION_ID)
        val scheduler = getScheduler(context)
        scheduler.cancel()
        AllowForegroundWorkersCommand(context).execute()
    }

    private fun getScheduler(context: Context): IOneTimeTaskScheduler {
        return WeatherUpdateWorker.scheduler(context)
    }

    const val WEATHER_NOTIFICATION_ID = 1
}