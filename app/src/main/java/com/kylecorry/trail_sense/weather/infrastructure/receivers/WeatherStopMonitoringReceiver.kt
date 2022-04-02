package com.kylecorry.trail_sense.weather.infrastructure.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.weather.infrastructure.commands.StopWeatherMonitorCommand

class WeatherStopMonitoringReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        StopWeatherMonitorCommand(context).execute()
    }

}