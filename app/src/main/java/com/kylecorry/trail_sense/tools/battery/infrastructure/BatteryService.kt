package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.battery.domain.RunningService
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightState
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightHandler
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightService
import java.time.Duration

class BatteryService {

    fun getRunningServices(context: Context): List<RunningService> {
        val prefs = UserPreferences(context)
        var services = mutableListOf<RunningService>()

        // Backtrack

        if (prefs.backtrackEnabled && !prefs.isLowPowerModeOn) {
            services.add(
                RunningService(
                    context.getString(R.string.tool_backtrack_title),
                    prefs.backtrackRecordFrequency
                )
            )
        }
        // Weather
        if (prefs.weather.shouldMonitorWeather && !prefs.isLowPowerModeOn) {
            services.add(
                RunningService(
                    context.getString(R.string.weather),
                    prefs.weather.weatherUpdateFrequency
                )
            )
        }

        // Sunset alerts
        if (prefs.astronomy.sendSunsetAlerts) {
            services.add(
                RunningService(
                    context.getString(R.string.pref_sunset_alerts_title),
                    Duration.ofDays(1)
                )
            )
        }

        if (FlashlightHandler.getInstance(context).getState() != FlashlightState.Off){
            services.add(
                RunningService(
                    context.getString(R.string.flashlight_title),
                    Duration.ZERO
                )
            )
        }

        return services
    }

}