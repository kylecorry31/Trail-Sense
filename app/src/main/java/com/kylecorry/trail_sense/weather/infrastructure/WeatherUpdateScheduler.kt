package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.jobs.IOneTimeTaskScheduler
import com.kylecorry.andromeda.notify.Notify

object WeatherUpdateScheduler {

    private fun isOn(context: Context): Boolean {
        return WeatherMonitorIsEnabled().isSatisfiedBy(context)
    }

    fun start(context: Context) {
        if (isOn(context)) {
            return
        }
        val scheduler = getScheduler(context)
        scheduler.once()
    }

    fun stop(context: Context) {
        Notify.cancel(context, WEATHER_NOTIFICATION_ID)
        val scheduler = getScheduler(context)
        scheduler.cancel()
    }

    private fun getScheduler(context: Context): IOneTimeTaskScheduler {
        return WeatherUpdateWorker.scheduler(context)
    }

    const val WEATHER_NOTIFICATION_ID = 1
}