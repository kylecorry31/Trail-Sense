package com.kylecorry.trail_sense.receivers

import android.content.Context
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler

object TrailSenseServiceUtils {

    fun restartServices(context: Context){
        startWeatherMonitoring(context)
        startSunsetAlarm(context)
        startBacktrack(context)
    }

    private fun startWeatherMonitoring(context: Context) {
        val prefs = UserPreferences(context)
        if (prefs.weather.shouldMonitorWeather) {
            WeatherUpdateScheduler.start(context)
        } else {
            WeatherUpdateScheduler.stop(context)
        }
    }

    private fun startBacktrack(context: Context){
        val prefs = UserPreferences(context)
        if (prefs.backtrackEnabled) {
            BacktrackScheduler.start(context)
        } else {
            BacktrackScheduler.stop(context)
        }
    }

    private fun startSunsetAlarm(context: Context) {
        context.sendBroadcast(SunsetAlarmReceiver.intent(context))
    }

}