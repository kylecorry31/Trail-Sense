package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.background.IAlwaysOnTaskScheduler
import com.kylecorry.andromeda.background.TaskSchedulerFactory
import com.kylecorry.andromeda.background.services.ForegroundInfo
import com.kylecorry.andromeda.background.services.IntervalService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.alerts.CurrentWeatherAlerter
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration

class WeatherMonitorService :
    IntervalService(wakelockDuration = Duration.ofSeconds(30), useOneTimeWorkers = true) {

    private val prefs by lazy { UserPreferences(applicationContext) }

    override fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            WeatherUpdateScheduler.WEATHER_NOTIFICATION_ID,
            CurrentWeatherAlerter.getDefaultNotification(applicationContext)
        )
    }

    override val uniqueId: Int
        get() = 2387092

    override val period: Duration
        get() = prefs.weather.weatherUpdateFrequency

    override suspend fun doWork() {
        WeatherSubsystem.getInstance(applicationContext).updateWeather()
    }

    override fun onDestroy() {
        stopService(true)
        super.onDestroy()
    }

    companion object {
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