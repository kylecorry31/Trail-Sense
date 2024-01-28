package com.kylecorry.trail_sense.tools.weather.infrastructure

import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.receivers.ServiceRestartAlerter
import com.kylecorry.trail_sense.shared.permissions.canStartLocationForgroundService

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

        if (!hasPermissions(context)) {
            ServiceRestartAlerter(context).alert()
            Log.d("WeatherUpdateScheduler", "Cannot start weather monitoring")
            return
        }

        WeatherMonitorService.start(context)
    }

    private fun hasPermissions(context: Context): Boolean {
        // Either it didn't need location or it has foreground location permission (runtime check)
        return !Permissions.canGetLocation(context) || Permissions.canStartLocationForgroundService(context)
    }

    fun stop(context: Context) {
        WeatherMonitorService.stop(context)
        Notify.cancel(context, WEATHER_NOTIFICATION_ID)
    }

    const val WEATHER_NOTIFICATION_ID = 1
}