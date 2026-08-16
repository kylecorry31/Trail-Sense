package com.kylecorry.trail_sense.tools.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration

object WeatherMonitorScheduler {

    fun restart(context: Context) {
        val service =
            Tools.getService(context, WeatherToolRegistration.SERVICE_WEATHER_MONITOR) ?: return
        if (service.isEnabled() && !service.isBlocked()) {
            stop(context)
            start(context)
        }
    }

    fun start(context: Context) {
        WeatherMonitorService.start(context)
    }

    fun stop(context: Context) {
        WeatherMonitorService.stop(context)
        Notify.cancel(context, WeatherMonitorService.WEATHER_NOTIFICATION_ID)
    }
}
