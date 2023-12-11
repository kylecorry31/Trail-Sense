package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.receivers.ServiceRestartAlerter
import com.kylecorry.trail_sense.shared.permissions.canRunLocationForegroundService

object WeatherUpdateScheduler {

    fun restart(context: Context) {
        if (WeatherMonitorIsEnabled().isSatisfiedBy(context)) {
            stop(context)
            start(context)
        }
    }

    fun start(context: Context, isInBackground: Boolean = false) {
        if (!WeatherMonitorIsAvailable().isSatisfiedBy(context)) {
            return
        }

        if (isInBackground && !canStartFromBackground(context)) {
            ServiceRestartAlerter(context).alert()
            Log.d("WeatherUpdateScheduler", "Cannot start weather monitoring")
            return
        }

        WeatherMonitorService.start(context)
    }

    private fun canStartFromBackground(context: Context): Boolean {
        // TODO: If it was started without permission, it should be able to be restarted without permission - keep track of this
        return Permissions.canRunLocationForegroundService(context)
    }

    fun stop(context: Context) {
        WeatherMonitorService.stop(context)
        Notify.cancel(context, WEATHER_NOTIFICATION_ID)
    }

    const val WEATHER_NOTIFICATION_ID = 1
}