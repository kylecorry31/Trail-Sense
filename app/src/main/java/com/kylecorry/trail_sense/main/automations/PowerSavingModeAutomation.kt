package com.kylecorry.trail_sense.main.automations

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.automations.Automation
import com.kylecorry.trail_sense.shared.automations.AutomationReceiver
import com.kylecorry.trail_sense.shared.automations.BooleanParameterTransformer
import com.kylecorry.trail_sense.tools.battery.BatteryToolRegistration
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration
import com.kylecorry.trail_sense.tools.weather.receivers.SetWeatherMonitorStateReceiver

object PowerSavingModeAutomation {

    fun create(context: Context): Automation {
        return Automation(
            BatteryToolRegistration.ACTION_POWER_SAVING_MODE_CHANGED,
            listOf(
                AutomationReceiver(
                    WeatherToolRegistration.RECEIVER_SET_WEATHER_MONITOR_STATE,
                    listOf(
                        BooleanParameterTransformer(
                            BatteryToolRegistration.PARAM_POWER_SAVING_MODE_ENABLED,
                            SetWeatherMonitorStateReceiver.PARAM_WEATHER_MONITOR_STATE,
                            invert = true
                        )
                    ),
                    UserPreferences(context).lowPowerModeDisablesWeather
                )
            )

        )
    }

}