package com.kylecorry.trail_sense.weather.infrastructure

import android.app.Notification
import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.services.CoroutineIntervalService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.alerts.CurrentWeatherAlerter
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration

class WeatherMonitorAlwaysOnService : CoroutineIntervalService(TAG) {

    private val prefs by lazy { UserPreferences(applicationContext) }

    override val foregroundNotificationId: Int
        get() = WeatherUpdateScheduler.WEATHER_NOTIFICATION_ID

    override val period: Duration
        get() = prefs.weather.weatherUpdateFrequency

    override suspend fun doWork() {
        WeatherSubsystem.getInstance(applicationContext).updateWeather(true)
    }

    override fun getForegroundNotification(): Notification {
        return CurrentWeatherAlerter.getDefaultNotification(applicationContext)
    }

    override fun onDestroy() {
        stopService(true)
        super.onDestroy()
    }

    companion object {
        const val TAG = "WeatherMonitorHighPriorityService"

        fun intent(context: Context): Intent {
            return Intent(context, WeatherMonitorAlwaysOnService::class.java)
        }

        fun start(context: Context) {
            Intents.startService(context, intent(context), foreground = true)
        }

        fun stop(context: Context) {
            context.stopService(intent(context))
        }

    }
}