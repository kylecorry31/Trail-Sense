package com.kylecorry.trail_sense.weather.infrastructure.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler

class WeatherStopMonitoringReceiver: BroadcastReceiver() {


    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val prefs = UserPreferences(context)
        prefs.weather.shouldMonitorWeather = false
        WeatherUpdateScheduler.stop(context)
    }

}