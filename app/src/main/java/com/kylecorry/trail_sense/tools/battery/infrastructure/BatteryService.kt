package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.content.Context
import com.kylecorry.andromeda.battery.IBattery
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackIsEnabled
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReading
import com.kylecorry.trail_sense.tools.battery.domain.PowerService
import com.kylecorry.trail_sense.tools.battery.domain.RunningService
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherMonitorIsEnabled
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import java.time.Duration

class BatteryService {

    private val powerService = PowerService()

    fun getRunningServices(context: Context): List<RunningService> {
        val prefs = UserPreferences(context)
        val services = mutableListOf<RunningService>()

        // Odometer
        if (prefs.pedometer.isEnabled && !prefs.isLowPowerModeOn) {
            services.add(
                RunningService(
                    context.getString(R.string.pedometer),
                    Duration.ZERO
                ) {
                    prefs.pedometer.isEnabled = false
                    StepCounterService.stop(context)
                }
            )
        }

        // Backtrack
        if (BacktrackIsEnabled().isSatisfiedBy(context)) {
            services.add(
                RunningService(
                    context.getString(R.string.backtrack),
                    prefs.backtrackRecordFrequency
                ) {
                    prefs.backtrackEnabled = false
                    BacktrackScheduler.stop(context)
                }
            )
        }

        // Weather
        if (WeatherMonitorIsEnabled().isSatisfiedBy(context)) {
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
                    context.getString(R.string.sunset_alerts),
                    Duration.ofDays(1)
                ) {
                    prefs.astronomy.sendSunsetAlerts = false
                }
            )
        }

        if (FlashlightSubsystem.getInstance(context).getMode() != FlashlightMode.Off) {
            services.add(
                RunningService(
                    context.getString(R.string.flashlight_title),
                    Duration.ZERO
                ) { FlashlightSubsystem.getInstance(context).set(FlashlightMode.Off) }
            )
        }

        return services
    }

    fun getTimeUntilEmpty(battery: IBattery, readings: List<BatteryReading>): Duration? {
        val capacity = battery.percent
        val rates = powerService.getRates(readings, Duration.ofMinutes(5), false)
        val lastDischargeRate = rates.lastOrNull { it < 0f } ?: return null
        return powerService.getTimeUntilEmpty(capacity, lastDischargeRate)
    }

    fun getTimeUntilFull(battery: IBattery, readings: List<BatteryReading>): Duration? {
        val capacity = battery.percent
        val rates = powerService.getRates(readings, Duration.ofMinutes(5), false)
        val lastChargeRate = rates.lastOrNull { it > 0f } ?: return null
        val maxCapacity = 100f
        return powerService.getTimeUntilFull(capacity, maxCapacity, lastChargeRate)
    }


}