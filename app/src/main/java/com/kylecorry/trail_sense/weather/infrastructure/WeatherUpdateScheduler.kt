package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.jobs.IOneTimeTaskScheduler
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.shared.permissions.AreForegroundWorkersAllowed

object WeatherUpdateScheduler {

    fun start(context: Context) {
        if (!WeatherMonitorIsAvailable().isSatisfiedBy(context)) {
            return
        }

        val scheduler = getScheduler(context)

        if (AreForegroundWorkersAllowed().isSatisfiedBy(context)) {
            WeatherMonitorAlwaysOnService.stop(context)
            scheduler.once()
        } else {
            // Default to always on if it can't run in the background
            WeatherMonitorAlwaysOnService.start(context)
            scheduler.cancel()
        }
    }

    fun stop(context: Context) {
        WeatherMonitorAlwaysOnService.stop(context)
        Notify.cancel(context, WEATHER_NOTIFICATION_ID)
        val scheduler = getScheduler(context)
        scheduler.cancel()
    }

    private fun getScheduler(context: Context): IOneTimeTaskScheduler {
        return WeatherUpdateWorker.scheduler(context)
    }

    const val WEATHER_NOTIFICATION_ID = 1
}