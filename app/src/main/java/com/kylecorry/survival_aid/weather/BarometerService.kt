package com.kylecorry.survival_aid.weather

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Handler
import com.kylecorry.survival_aid.R


class BarometerService: Service() {

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var broadcastIntent: Intent

    override fun onCreate() {
        handler = Handler()
        broadcastIntent = Intent(this, BarometerAlarmReceiver::class.java)

        createNotificationChannel()
        val notification = Notification.Builder(this, "Barometer")
            .setContentTitle("Barometer")
            .setContentText("Monitoring barometer in the background")
            .setSmallIcon(R.drawable.ic_weather)
            .build()

        startForeground(1, notification)

        val delay = 15 * 60 * 1000L // 15 Minutes

        runnable = Runnable {
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY_COMPATIBILITY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Barometer"
            val descriptionText = "Barometer recording service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("Barometer", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {

        private var started = false

        fun start(context: Context){
            if (started) return

            context.startForegroundService(Intent(context, BarometerService::class.java))
        }
    }

}