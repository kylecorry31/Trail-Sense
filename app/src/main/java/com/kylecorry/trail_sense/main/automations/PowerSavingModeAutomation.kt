package com.kylecorry.trail_sense.main.automations

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.automations.Automation
import com.kylecorry.trail_sense.tools.battery.BatteryToolRegistration
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration

object PowerSavingModeAutomation {

    fun onEnabled(context: Context): Automation {
        val prefs = UserPreferences(context)
        return Automation(
            BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_ENABLED,
            listOfNotNull(
                if (prefs.lowPowerModeDisablesWeather) WeatherToolRegistration.ACTION_PAUSE_WEATHER_MONITOR else null,
                if (prefs.lowPowerModeDisablesBacktrack) PathsToolRegistration.ACTION_PAUSE_BACKTRACK else null,
                PedometerToolRegistration.ACTION_PAUSE_PEDOMETER
            )
        )
    }

    fun onDisabled(): Automation {
        return Automation(
            BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_DISABLED,
            listOf(
                WeatherToolRegistration.ACTION_RESUME_WEATHER_MONITOR,
                PathsToolRegistration.ACTION_RESUME_BACKTRACK,
                PedometerToolRegistration.ACTION_RESUME_PEDOMETER
            )
        )
    }
}