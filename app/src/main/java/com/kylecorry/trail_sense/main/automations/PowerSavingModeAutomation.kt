package com.kylecorry.trail_sense.main.automations

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.automations.Automation
import com.kylecorry.trail_sense.shared.automations.AutomationReceiver
import com.kylecorry.trail_sense.tools.battery.BatteryToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration

object PowerSavingModeAutomation {

    fun onEnabled(context: Context): Automation {
        return Automation(
            BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_ENABLED,
            listOf(
                AutomationReceiver(
                    WeatherToolRegistration.ACTION_PAUSE_WEATHER_MONITOR,
                    enabled = UserPreferences(context).lowPowerModeDisablesWeather
                ),
                AutomationReceiver(PedometerToolRegistration.ACTION_PAUSE_PEDOMETER)
            )
        )
    }

    fun onDisabled(context: Context): Automation {
        return Automation(
            BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_DISABLED,
            listOf(
                AutomationReceiver(WeatherToolRegistration.ACTION_RESUME_WEATHER_MONITOR),
                AutomationReceiver(PedometerToolRegistration.ACTION_RESUME_PEDOMETER)
            )
        )
    }
}