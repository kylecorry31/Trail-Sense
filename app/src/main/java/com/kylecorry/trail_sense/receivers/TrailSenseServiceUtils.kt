package com.kylecorry.trail_sense.receivers

import android.content.Context
import android.os.Build
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.tiles.TileManager
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.AstronomyDailyWorker
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryLogWorker
import com.kylecorry.trail_sense.tools.paths.infrastructure.services.BacktrackService
import com.kylecorry.trail_sense.tools.paths.infrastructure.subsystem.BacktrackSubsystem
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorService
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherUpdateScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TrailSenseServiceUtils {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun restartServices(context: Context, isInBackground: Boolean = false) {
        val appContext = context.applicationContext
        coroutineScope.launch {
            if (!isInBackground) {
                ServiceRestartAlerter(appContext).dismiss()
            }

            startWeatherMonitoring(appContext)
            startBacktrack(appContext)
            startPedometer(appContext)
            startSunsetAlarm(appContext)
            startAstronomyAlerts(appContext)
            BatteryLogWorker.start(appContext)
            TileManager().setTilesEnabled(
                appContext,
                UserPreferences(appContext).power.areTilesEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            )
        }
    }

    /**
     * Temporarily stops all services (will restart when the app is opened again)
     */
    fun stopServices(context: Context) {
        val appContext = context.applicationContext
        WeatherUpdateScheduler.stop(appContext)
        BacktrackService.stop(appContext)
        StepCounterService.stop(appContext)
        AstronomyDailyWorker.stop(appContext)
        BatteryLogWorker.stop(appContext)
        SunsetAlarmReceiver.scheduler(appContext).cancel()
        TileManager().setTilesEnabled(appContext, false)
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
            if (!WeatherMonitorService.isRunning) {
                WeatherUpdateScheduler.start(context)
            }
        } else {
            WeatherUpdateScheduler.stop(context)
        }
    }

    private suspend fun startBacktrack(context: Context) {
        val backtrack = BacktrackSubsystem.getInstance(context)
        if (backtrack.getState() == FeatureState.On) {
            if (!BacktrackService.isRunning) {
                backtrack.enable(false)
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