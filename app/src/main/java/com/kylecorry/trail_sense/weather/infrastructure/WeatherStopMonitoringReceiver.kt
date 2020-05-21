package com.kylecorry.trail_sense.weather.infrastructure

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WeatherStopMonitoringReceiver: BroadcastReceiver() {


    override fun onReceive(context: Context?, intent: Intent?) {
        context?.stopService(Intent(context, BarometerService::class.java))
    }

}