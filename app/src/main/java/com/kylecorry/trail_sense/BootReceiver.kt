package com.kylecorry.trail_sense

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.astronomy.infrastructure.SunsetAlarmReceiver
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.BarometerService

class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null){
            val prefs = UserPreferences(context)

            // Start the weather service
            if (prefs.weather.shouldMonitorWeather) {
                BarometerService.start(
                    context
                )
            } else {
                BarometerService.stop(
                    context
                )
            }

            // Start the sunset alarm
            context.sendBroadcast(SunsetAlarmReceiver.intent(context))
        }
    }
}