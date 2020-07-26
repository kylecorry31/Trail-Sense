package com.kylecorry.trail_sense.weather.infrastructure

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.MainActivity
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
        broadcastIntent = Intent(this, BarometerAlarmReceiver::class.java)

        createNotificationChannel()

        val stopIntent = Intent(this, WeatherStopMonitoringReceiver::class.java)
        val openIntent = Intent(this, MainActivity::class.java)

        val stopPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, stopIntent, 0)
        val openPendingIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, openIntent, 0)

        val stopAction = Notification.Action.Builder(
                Icon.createWithResource("", R.drawable.ic_cancel),
                getString(R.string.stop_monitoring),
                stopPendingIntent)
            .build()

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "Weather")
                .setContentTitle("Weather")
                .setContentText("Monitoring weather in the background")
                .setSmallIcon(R.drawable.ic_weather)
                .addAction(stopAction)
                .setContentIntent(openPendingIntent)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Weather")
                .setContentText("Monitoring weather in the background")
                .setSmallIcon(R.drawable.ic_weather)
                .addAction(stopAction)
                .setContentIntent(openPendingIntent)
                .build()
        }

        startForeground(1, notification)

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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Weather"
            val descriptionText = "Weather monitoring in the background"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("Weather", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService<NotificationManager>()
            notificationManager?.createNotificationChannel(channel)
        }
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