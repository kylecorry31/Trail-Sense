package com.kylecorry.trail_sense.weather.infrastructure

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.shared.sensors.SensorChecker

class BarometerBootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.intent.action.BOOT_COMPLETED" && context != null){
            val sensorChecker = SensorChecker(context)
            if (sensorChecker.hasBarometer()) {
                BarometerService.start(context)
            }
        }
    }
}