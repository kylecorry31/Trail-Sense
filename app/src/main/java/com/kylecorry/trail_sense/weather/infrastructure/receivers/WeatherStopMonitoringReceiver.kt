package com.kylecorry.trail_sense.weather.infrastructure.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.weather.infrastructure.WeatherAlarmScheduler

class WeatherStopMonitoringReceiver: BroadcastReceiver() {


    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null){
            return
        }
        WeatherAlarmScheduler.stop(context)
    }

}