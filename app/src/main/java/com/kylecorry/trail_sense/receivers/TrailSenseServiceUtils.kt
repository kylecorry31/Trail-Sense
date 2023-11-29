package com.kylecorry.trail_sense.receivers

import android.content.Context
import android.os.Build
import com.kylecorry.trail_sense.astronomy.infrastructure.AstronomyDailyWorker
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.navigation.paths.infrastructure.services.BacktrackService
import com.kylecorry.trail_sense.navigation.paths.infrastructure.subsystem.BacktrackSubsystem
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tiles.TileManager
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryLogWorker
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherMonitorService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TrailSenseServiceUtils {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun restartServices(context: Context, isInBackground: Boolean = false) {
        coroutineScope.launch {
            if (!isInBackground){
                ServiceRestartAlerter(context).dismiss()
            }

            startWeatherMonitoring(context, isInBackground)
            startBacktrack(context, isInBackground)
            startPedometer(context)
            startSunsetAlarm(context)
            startAstronomyAlerts(context)
            BatteryLogWorker.start(context)
            TileManager().setTilesEnabled(
                context,
                UserPreferences(context).power.areTilesEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            )
        }
    }

    /**
     * Temporarily stops all services (will restart when the app is opened again)
     */
    fun stopServices(context: Context) {
        WeatherUpdateScheduler.stop(context)
        BacktrackService.stop(context)
        StepCounterService.stop(context)
        AstronomyDailyWorker.stop(context)
        BatteryLogWorker.stop(context)
        SunsetAlarmReceiver.scheduler(context).cancel()
        TileManager().setTilesEnabled(context, false)
    }

    private fun startPedometer(context: Context) {
        val prefs = UserPreferences(context)
        if (prefs.pedometer.isEnabled) {
            StepCounterService.start(context)
        } else {
            StepCounterService.stop(context)
        }
    }

    private fun startWeatherMonitoring(context: Context, isInBackground: Boolean) {
        val prefs = UserPreferences(context)
        if (prefs.weather.shouldMonitorWeather) {
            if (!WeatherMonitorService.isRunning) {
                WeatherUpdateScheduler.start(context, isInBackground)
            }
        } else {
            WeatherUpdateScheduler.stop(context)
        }
    }

    private suspend fun startBacktrack(context: Context, isInBackground: Boolean) {
        val backtrack = BacktrackSubsystem.getInstance(context)
        if (backtrack.getState() == FeatureState.On) {
            if (!BacktrackService.isRunning) {
                backtrack.enable(false, isInBackground)
            }
        } else {
            backtrack.disable()
        }
    }

    private fun startSunsetAlarm(context: Context) {
        SunsetAlarmReceiver.start(context)
    }

    private fun startAstronomyAlerts(context: Context) {
        AstronomyDailyWorker.start(context)
    }

}