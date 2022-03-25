package com.kylecorry.trail_sense.receivers

import android.content.Context
import android.os.Build
import com.kylecorry.trail_sense.astronomy.infrastructure.AstronomyDailyWorker
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.main.LaunchFromBackgroundService
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.IsBatteryExemptionRequired
import com.kylecorry.trail_sense.tiles.TileManager
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryLogWorker
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler

object TrailSenseServiceUtils {

    fun restartServices(context: Context) {
        if (IsBatteryExemptionRequired().isSatisfiedBy(context)) {
            LaunchFromBackgroundService.start(context)
        } else {
            LaunchFromBackgroundService.stop(context)
        }

        startWeatherMonitoring(context)
        startSunsetAlarm(context)
        startAstronomyAlerts(context)
        startBacktrack(context)
        startPedometer(context)
        BatteryLogWorker.scheduler(context).once()
        TileManager().setTilesEnabled(
            context,
            UserPreferences(context).power.areTilesEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        )
    }

    private fun startPedometer(context: Context) {
        val prefs = UserPreferences(context)
        if (prefs.pedometer.isEnabled) {
            StepCounterService.start(context)
        } else {
            StepCounterService.stop(context)
        }
    }

    private fun startWeatherMonitoring(context: Context) {
        val prefs = UserPreferences(context)
        if (prefs.weather.shouldMonitorWeather) {
            WeatherUpdateScheduler.start(context)
        } else {
            WeatherUpdateScheduler.stop(context)
        }
    }

    private fun startBacktrack(context: Context) {
        val prefs = UserPreferences(context)
        if (prefs.backtrackEnabled) {
            BacktrackScheduler.start(context, false)
        } else {
            BacktrackScheduler.stop(context)
        }
    }

    private fun startSunsetAlarm(context: Context) {
        SunsetAlarmReceiver.start(context)
    }

    private fun startAstronomyAlerts(context: Context) {
        AstronomyDailyWorker.start(context)
    }

}