package com.kylecorry.trail_sense.shared

import android.app.Activity
import android.content.Context
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler

class LowPowerMode(val context: Context) {

    private val prefs by lazy { UserPreferences(context) }

    fun enable(activity: Activity? = null) {
        prefs.isLowPowerModeOn = true
        if (prefs.lowPowerModeDisablesWeather) {
            WeatherUpdateScheduler.stop(context)
        }

        if (prefs.lowPowerModeDisablesBacktrack) {
            BacktrackScheduler.stop(context)
        }
        activity?.recreate()
    }

    fun disable(activity: Activity? = null) {
        prefs.isLowPowerModeOn = false

        if (activity != null){
            activity.recreate()
            return
        }

        // Only need to be restarted if the activity doesn't get recreated
        if (prefs.weather.shouldMonitorWeather) {
            WeatherUpdateScheduler.start(context)
        }

        if (prefs.backtrackEnabled) {
            BacktrackScheduler.start(context)
        }
    }

    fun isEnabled(): Boolean {
        return prefs.isLowPowerModeOn
    }
}