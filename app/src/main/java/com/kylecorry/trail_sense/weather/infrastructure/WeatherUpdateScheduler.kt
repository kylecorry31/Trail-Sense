package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.notify.Notify

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

        WeatherMonitorService.start(context)
    }

    fun stop(context: Context) {
        WeatherMonitorService.stop(context)
        Notify.cancel(context, WEATHER_NOTIFICATION_ID)
    }

    const val WEATHER_NOTIFICATION_ID = 1
}