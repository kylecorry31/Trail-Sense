package com.kylecorry.trail_sense.weather.infrastructure.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.WeatherNotificationService
import com.kylecorry.trail_sense.weather.infrastructure.receivers.WeatherUpdateService
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer

class WeatherUpdateForegroundService : Service() {

    private val intervalometer = Intervalometer {
        startService(WeatherUpdateService.intent(applicationContext))
        Log.i(WeatherUpdateForegroundService::class.simpleName, "Broadcast sent")
    }

    private val prefs by lazy { UserPreferences(this) }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        started = true
        val notification = WeatherNotificationService.getNotification(
            this,
            getString(R.string.notification_monitoring_weather),
            R.drawable.ic_weather
        )
        startForeground(WeatherNotificationService.WEATHER_NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intervalometer.interval(prefs.weather.weatherUpdateFrequency)
        return START_STICKY_COMPATIBILITY
    }

    override fun onDestroy() {
        intervalometer.stop()
        stopForeground(true)
        started = false
        super.onDestroy()
    }

    companion object {

        private var started = false

        fun intent(context: Context): Intent {
            return Intent(context, WeatherUpdateForegroundService::class.java)
        }

        fun start(context: Context) {
            if (started) {
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent(context))
            } else {
                context.startService(intent(context))
            }
        }

        fun stop(context: Context) {
            context.stopService(intent(context))
        }
    }
}