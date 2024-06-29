package com.kylecorry.trail_sense.tools.weather.receivers

import android.content.Context
import android.os.Bundle
import com.kylecorry.trail_sense.tools.battery.BatteryToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.AutomationReceiver
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorIsEnabled
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherUpdateScheduler

// TODO: Make this more generic - ChangeWeatherMonitorStateReceiver
// To do so, there probably needs to be a converter between the broadcast and receiver which converts the low power mode state to the enabled state
class WeatherMonitorPowerSavingModeReceiver : AutomationReceiver {
    override fun onReceive(context: Context, data: Bundle) {
        val isLowPower = data.getBoolean(BatteryToolRegistration.PARAM_POWER_SAVING_MODE_ENABLED)

        if (isLowPower) {
            WeatherUpdateScheduler.stop(context)
        } else if (WeatherMonitorIsEnabled().isSatisfiedBy(context)) {
            WeatherUpdateScheduler.start(context)
        }
    }
}