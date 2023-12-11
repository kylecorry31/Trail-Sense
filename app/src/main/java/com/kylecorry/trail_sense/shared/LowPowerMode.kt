package com.kylecorry.trail_sense.shared

import android.app.Activity
import android.content.Context
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherMonitorIsEnabled
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LowPowerMode(val context: Context) {

    private val prefs by lazy { UserPreferences(context) }
    private val scope = CoroutineScope(Dispatchers.Default)

    fun enable(activity: Activity? = null) {
        prefs.isLowPowerModeOn = true
        if (prefs.lowPowerModeDisablesWeather) {
            WeatherUpdateScheduler.stop(context)
        }

        if (prefs.lowPowerModeDisablesBacktrack) {
            BacktrackScheduler.stop(context)
        }

        StepCounterService.stop(context)

        activity?.recreate()
    }

    fun disable(activity: Activity? = null) {
        prefs.isLowPowerModeOn = false

        if (activity != null){
            activity.recreate()
            return
        }

        scope.launch {
            // Only need to be restarted if the activity doesn't get recreated
            if (WeatherMonitorIsEnabled().isSatisfiedBy(context)) {
                WeatherUpdateScheduler.start(context)
            }

            if (BacktrackScheduler.isOn(context)) {
                BacktrackScheduler.start(context, false)
            }

            if (prefs.pedometer.isEnabled) {
                StepCounterService.start(context)
            }
        }
    }

    fun isEnabled(): Boolean {
        return prefs.isLowPowerModeOn
    }
}