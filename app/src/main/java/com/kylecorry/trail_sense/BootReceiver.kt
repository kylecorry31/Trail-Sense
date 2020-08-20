package com.kylecorry.trail_sense

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.astronomy.infrastructure.SunsetAlarmReceiver
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.BarometerService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {
            startWeatherMonitoring(context)
            startSunsetAlarm(context)
        }
    }

    private fun startWeatherMonitoring(context: Context) {
        val prefs = UserPreferences(context)
        if (prefs.weather.shouldMonitorWeather) {
            BarometerService.start(context)
        } else {
            BarometerService.stop(context)
        }
    }

    private fun startSunsetAlarm(context: Context) {
        context.sendBroadcast(SunsetAlarmReceiver.intent(context))
    }
}