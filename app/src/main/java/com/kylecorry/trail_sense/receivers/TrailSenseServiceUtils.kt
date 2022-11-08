package com.kylecorry.trail_sense.receivers

import android.content.Context
import android.os.Build
import com.kylecorry.trail_sense.navigation.paths.infrastructure.subsystem.BacktrackSubsystem
import com.kylecorry.trail_sense.shared.Background
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.AllowForegroundWorkersCommand
import com.kylecorry.trail_sense.tiles.TileManager
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler

object TrailSenseServiceUtils {

    fun restartServices(context: Context) {
        AllowForegroundWorkersCommand(context).execute()
        startWeatherMonitoring(context)
        startSunsetAlarm(context)
        startAstronomyAlerts(context)
        startBacktrack(context)
        startPedometer(context)
        Background.start(context, Background.BatteryLogger)
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
        val backtrack = BacktrackSubsystem.getInstance(context)
        if (backtrack.getState() == FeatureState.On) {
            backtrack.enable(false)
        } else {
            backtrack.disable()
        }
    }

    private fun startSunsetAlarm(context: Context) {
        Background.start(context, Background.SunsetAlerts)
    }

    private fun startAstronomyAlerts(context: Context) {
        Background.start(context, Background.AstronomyAlerts)
    }

}