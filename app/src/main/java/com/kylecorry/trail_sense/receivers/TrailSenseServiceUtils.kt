package com.kylecorry.trail_sense.receivers

import android.content.Context
import android.os.Build
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.AstronomyAlertReceiver
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.settings.migrations.PreferenceMigrator
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tiles.TileManager
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryLogWorker
import com.kylecorry.trail_sense.tools.speedometer.infrastructure.PedometerService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import java.time.Duration

object TrailSenseServiceUtils {

    fun restartServices(context: Context) {
        PreferenceMigrator.getInstance().migrate(context)
        NotificationChannels.createChannels(context)
        startWeatherMonitoring(context)
        startSunsetAlarm(context)
        startAstronomyAlerts(context)
        startBacktrack(context)
        startPedometer(context)
        BatteryLogWorker.scheduler(context).schedule(Duration.ZERO)
        TileManager().setTilesEnabled(
            context,
            UserPreferences(context).power.areTilesEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        )
    }

    private fun startPedometer(context: Context) {
        val prefs = UserPreferences(context)
        if (prefs.usePedometer) {
            PedometerService.start(context)
        } else {
            PedometerService.stop(context)
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
        context.sendBroadcast(SunsetAlarmReceiver.intent(context))
    }

    private fun startAstronomyAlerts(context: Context) {
        AstronomyAlertReceiver.start(context)
    }

}