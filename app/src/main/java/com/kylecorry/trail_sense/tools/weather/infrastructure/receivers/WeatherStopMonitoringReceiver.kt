package com.kylecorry.trail_sense.tools.weather.infrastructure.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration
import kotlinx.coroutines.runBlocking

class WeatherStopMonitoringReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        runBlocking {
            Tools.getService(context, WeatherToolRegistration.SERVICE_WEATHER_MONITOR)?.disable()
        }
    }

}