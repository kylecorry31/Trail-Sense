package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.infrastructure.SunsetAlarmService
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.battery.domain.RunningService
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightState
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightHandler
import com.kylecorry.trail_sense.tools.speedometer.infrastructure.PedometerService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import java.time.Duration

class BatteryService {

    fun getRunningServices(context: Context): List<RunningService> {
        val prefs = UserPreferences(context)
        val services = mutableListOf<RunningService>()

        // Odometer
        if (prefs.usePedometer && !prefs.isLowPowerModeOn) {
            services.add(
                RunningService(
                    context.getString(R.string.pedometer),
                    Duration.ZERO
                ) {
                    prefs.usePedometer = false
                    PedometerService.stop(context)
                }
            )
        }

        // Backtrack

        if (prefs.backtrackEnabled && !prefs.isLowPowerModeOn) {
            services.add(
                RunningService(
                    context.getString(R.string.tool_backtrack_title),
                    prefs.backtrackRecordFrequency
                ) {
                    prefs.backtrackEnabled = false
                    BacktrackScheduler.stop(context)
                }
            )
        }
        // Weather
        if (prefs.weather.shouldMonitorWeather && !prefs.isLowPowerModeOn) {
            services.add(
                RunningService(
                    context.getString(R.string.weather),
                    prefs.weather.weatherUpdateFrequency
                ) {
                    prefs.weather.shouldMonitorWeather = false
                    WeatherUpdateScheduler.stop(context)
                }
            )
        }

        // Sunset alerts
        if (prefs.astronomy.sendSunsetAlerts) {
            services.add(
                RunningService(
                    context.getString(R.string.pref_sunset_alerts_title),
                    Duration.ofDays(1)
                ) {
                    prefs.astronomy.sendSunsetAlerts = false
                }
            )
        }

        if (FlashlightHandler.getInstance(context).getState() != FlashlightState.Off) {
            services.add(
                RunningService(
                    context.getString(R.string.flashlight_title),
                    Duration.ZERO
                ) { FlashlightHandler.getInstance(context).off() }
            )
        }

        return services
    }

}