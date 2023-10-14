package com.kylecorry.trail_sense.weather.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import com.kylecorry.andromeda.background.IAlwaysOnTaskScheduler
import com.kylecorry.andromeda.background.TaskSchedulerFactory
import com.kylecorry.andromeda.background.services.ForegroundInfo
import com.kylecorry.andromeda.background.services.IntervalService
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.canRunLocationForegroundService
import com.kylecorry.trail_sense.weather.infrastructure.alerts.CurrentWeatherAlerter
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration

class WeatherMonitorService :
    IntervalService(wakelockDuration = Duration.ofSeconds(30), useOneTimeWorkers = true) {

    private val prefs by lazy { UserPreferences(applicationContext) }

    @SuppressLint("InlinedApi")
    override fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            WeatherUpdateScheduler.WEATHER_NOTIFICATION_ID,
            CurrentWeatherAlerter.getDefaultNotification(applicationContext),
            if (!Permissions.canRunLocationForegroundService(applicationContext)) {
                listOf(ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                // This shouldn't be needed, but I was seeing several crashes on Android 14 around security exceptions (might be a known bug in Android), so I'm adding it to experiment
                listOf(
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            }
        )
    }

    override val uniqueId: Int
        get() = 2387092

    override val period: Duration
        get() = prefs.weather.weatherUpdateFrequency

    override suspend fun doWork() {
        WeatherSubsystem.getInstance(applicationContext).updateWeather()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isRunning = false
        stopService(true)
        super.onDestroy()
    }

    companion object {

        var isRunning = false
            private set

        fun start(context: Context) {
            scheduler(context).start()
        }

        fun stop(context: Context) {
            scheduler(context).cancel()
        }

        fun scheduler(context: Context): IAlwaysOnTaskScheduler {
            return TaskSchedulerFactory(context).alwaysOn(
                WeatherMonitorService::class.java,
                foreground = true
            )
        }

    }
}