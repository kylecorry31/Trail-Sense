package com.kylecorry.trail_sense.weather.infrastructure

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import java.time.Duration
import java.time.ZonedDateTime


class BarometerService: Service() {

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var broadcastIntent: Intent

    private val INTERVAL = Duration.ofMinutes(10)

    override fun onCreate() {
        handler = Handler()
        broadcastIntent = WeatherUpdateReceiver.intent(this)

        val notification = WeatherNotificationService.getDefaultNotification(this)

        startForeground(WeatherNotificationService.WEATHER_NOTIFICATION_ID, notification)

        val delay = INTERVAL.toMillis()

        runnable = Runnable {
            Log.i("BarometerService", "Sent broadcast at " + ZonedDateTime.now().toString())
            sendBroadcast(broadcastIntent)
            handler.postDelayed(runnable, delay)
        }

        handler.post(runnable)
        started = true
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        stopForeground(true)
        started = false
        Log.i("BarometerService", "Weather monitoring stopped")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit {
            putBoolean(getString(R.string.pref_just_sent_alert), false)
        }
        return START_STICKY_COMPATIBILITY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {

        private var started = false

        fun start(context: Context){
            if (started) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, BarometerService::class.java))
            } else {
                context.startService(Intent(context, BarometerService::class.java))
            }
        }

        fun stop(context: Context){
            context.stopService(Intent(context, BarometerService::class.java))
        }
    }

}