package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.shared.Background
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.AllowForegroundWorkersCommand

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

        val prefs = UserPreferences(context)

        Background.start(context, Background.WeatherMonitor, prefs.weather.weatherUpdateFrequency)
    }

    fun stop(context: Context) {
        Background.stop(context, Background.WeatherMonitor)
        Notify.cancel(context, WEATHER_NOTIFICATION_ID)
        AllowForegroundWorkersCommand(context).execute()
    }

    const val WEATHER_NOTIFICATION_ID = 1
}